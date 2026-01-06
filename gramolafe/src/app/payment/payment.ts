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

  constructor(
    private route: ActivatedRoute, 
    private router: Router, 
    private http: HttpClient
  ) {}

  async ngOnInit() {
    // 1. Recuperamos parámetros de la URL enviados por el redirect del backend
    this.emailURL = this.route.snapshot.queryParamMap.get('email');
    this.tokenURL = this.route.snapshot.queryParamMap.get('token');
    
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
          if (this.listaPrecios.length > 0) this.planSeleccionado = this.listaPrecios[0];
        }
      },
      error: () => this.mensaje = "Error al conectar con la base de datos de precios."
    });

    // 4. Inicializar Stripe
    this.stripe = await loadStripe('pk_test_TYooMQauvdEDq54NiTphI7jx'); 
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

    // Generar token de tarjeta seguro en Stripe
    const { token, error } = await this.stripe.createToken(this.cardElement);

    if (error) {
      this.mensaje = "❌ " + error.message;
      return;
    }

    // 5. Construimos el cuerpo asegurando que no haya valores null críticos para el Backend
    const body = {
      tokenId: token.id,
      email: this.emailURL,
      tokenConfirmacion: this.tokenURL, 
      planId: this.planSeleccionado.id,
      amount: Math.round(this.planSeleccionado.importe * 100), 
      // Mapeo exacto para que MusicService no de error al insertar
      songTitle: this.songData ? this.songData.name : null,
      songArtist: this.songData ? this.songData.artists[0]?.name : null,
      songCover: this.songData ? this.songData.album?.images[0]?.url : null
    };

    this.http.post('http://localhost:8080/users/pay', body).subscribe({
      next: () => {
        if (this.songData) {
          sessionStorage.setItem("lastPayment", JSON.stringify({ titulo: this.songData.name }));
          sessionStorage.removeItem("pendingSong");
        }

        this.mensaje = "¡Pago realizado con éxito! ✅";
        
        setTimeout(() => {
          if (this.songData) {
            this.router.navigate(['/jukebox', this.emailURL]);
          } else {
            // Tras pagar la suscripción, el bar ya puede loguearse (Sección 2.4)
            this.router.navigate(['/login']);
          }
        }, 2000);
      },
      error: (err) => {
        this.mensaje = "❌ Error al procesar el pago: " + (err.error?.message || "Servidor no disponible");
      }
    });
  }
}