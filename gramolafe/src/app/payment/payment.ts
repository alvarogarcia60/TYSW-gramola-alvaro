import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { loadStripe, Stripe, StripeCardElement } from '@stripe/stripe-js';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './payment.html',
  styleUrl: './payment.css'
})
export class PaymentComponent implements OnInit {
  stripe: Stripe | null = null;
  cardElement: StripeCardElement | null = null;
  
  emailURL: string | null = null;
  tokenURL: string | null = null; 
  listaPrecios: any[] = [];
  planSeleccionado: any = null;
  songData: any = null; 
  mensaje = '';
  planIdURL: string | null = null;

  constructor(
    private route: ActivatedRoute, 
    private router: Router, 
    private http: HttpClient
  ) {}

  async ngOnInit() {
    // 1. Recuperamos parámetros de la URL enviados por el redirect del backend
    this.emailURL = this.route.snapshot.queryParamMap.get('email');
    this.tokenURL = this.route.snapshot.queryParamMap.get('token');
    this.planIdURL = this.route.snapshot.queryParamMap.get('planId');
    
    // 2. Verificamos si hay una canción pendiente de un cliente (Sección 4.6)
    const pending = sessionStorage.getItem("pendingSong");
    if (pending) {
      this.songData = JSON.parse(pending);
    }

    if (!this.emailURL) {
      this.mensaje = "⚠️ Acceso inválido. No se detectó el email del bar.";
      return; 
    }

    // 3. Cargar precios desde la BD (Suscripciones y Canción)
    this.http.get<any[]>('http://localhost:8080/precios/lista').subscribe({
      next: (data) => {
        this.listaPrecios = data;
        if (this.songData) {
          // Si hay canción pendiente, forzamos el plan de CANCIÓN
          this.planSeleccionado = data.find(p => p.id === 'CANCIÓN');
        } else {
          // Si no, filtramos por planes de suscripción para el dueño (Sección 2.4)
          this.listaPrecios = data.filter(p => p.id.includes('SUB'));
          // Si viene un planId en la URL, preselecciónalo
          if (this.planIdURL) {
            const encontrado = data.find(p => p.id === this.planIdURL);
            if (encontrado) {
              this.planSeleccionado = encontrado;
            }
          }
          // Fallback al primero si no hay preselección
          if (!this.planSeleccionado && this.listaPrecios.length > 0) {
            this.planSeleccionado = this.listaPrecios[0];
          }
        }
      },
      error: () => this.mensaje = "Error al conectar con la base de datos de precios."
    });

    // 4. Inicializar Stripe con clave de prueba estándar
    this.stripe = await loadStripe('pk_test_51SjrPwQFL6pDR5LT05J4blsEUb2dnn9HZRmQnYvpZ3BRYngtzQO5tXvjVFafpKkrxMI0vhGN5cXWaNdGc7BP3mHx00ZDfpKglz'); 
    if (this.stripe) {
      const elements = this.stripe.elements();
      this.cardElement = elements.create('card', {
        style: { base: { color: '#ffffff', fontSize: '16px' } },
        hidePostalCode: true 
      });
      this.cardElement.mount('#card-element');
    }
  }

  seleccionarPlan(plan: any) {
    this.planSeleccionado = plan;
  }

  async confirmarPago() {
    if (!this.stripe || !this.cardElement || !this.planSeleccionado) return;
    this.mensaje = "Procesando pago seguro... ⏳";

    // Si es pago de canción, usar PaymentIntent (nuevo flujo 4.6)
    if (this.songData) {
      const prepayBody = {
        email: this.emailURL,
        amount: this.planSeleccionado.importe
      };

      this.http.post('http://localhost:8080/payments/prepay', prepayBody, { responseType: 'text' }).subscribe({
        next: async (raw) => {
          const intent = JSON.parse(raw);
          const clientSecret = intent.client_secret;

          const result = await this.stripe!.confirmCardPayment(clientSecret, {
            payment_method: { card: this.cardElement! }
          });

          if (result.error) {
            this.mensaje = "❌ " + result.error.message;
            return;
          }

          const paymentIntentId = result.paymentIntent?.id;
          if (!paymentIntentId) {
            this.mensaje = "❌ No se pudo confirmar el pago";
            return;
          }

          // Añadir canción en posición 2 tras pago verificado
          this.http.post('http://localhost:8080/music/add-paid', {
            email: this.emailURL,
            paymentIntentId,
            songData: this.songData
          }).subscribe({
            next: () => {
              sessionStorage.setItem("lastPayment", JSON.stringify({ titulo: this.songData.name }));
              sessionStorage.removeItem("pendingSong");
              this.mensaje = "¡Pago realizado y canción colada en la cola! ✅";
              setTimeout(() => this.router.navigate(['/jukebox', this.emailURL]), 1500);
            },
            error: (err) => {
              this.mensaje = "❌ Error añadiendo la canción tras el pago: " + (err.error?.message || "Servidor no disponible");
            }
          });
        },
        error: (err) => {
          this.mensaje = "❌ Error al iniciar el pago: " + (err.error?.message || "Servidor no disponible");
        }
      });
      return;
    }

    // Si no hay canción (suscripción del dueño), mantener flujo existente
    const { token, error } = await this.stripe.createToken(this.cardElement);
    if (error) {
      this.mensaje = "❌ " + error.message;
      return;
    }
    const body = {
      tokenId: token!.id,
      email: this.emailURL,
      tokenConfirmacion: this.tokenURL,
      planId: this.planSeleccionado.id,
      amount: Math.round(this.planSeleccionado.importe * 100)
    };
    this.http.post('http://localhost:8080/users/pay', body).subscribe({
      next: () => {
        this.mensaje = "¡Pago realizado con éxito! ✅";
        setTimeout(() => this.router.navigate(['/login']), 1500);
      },
      error: (err) => {
        this.mensaje = "❌ Error al procesar el pago: " + (err.error?.message || "Servidor no disponible");
      }
    });
  }
}