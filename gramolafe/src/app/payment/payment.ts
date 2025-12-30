import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { PaymentService } from '../payment.service'; 

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './payment.html',
  styleUrls: ['./payment.css'] 
})
export class PaymentComponent implements OnInit {
  token: string | null = null;
  email: string | null = null;

  constructor(private route: ActivatedRoute, private paymentService: PaymentService) {}

  ngOnInit() {
    this.token = this.route.snapshot.queryParamMap.get('token');
    this.email = this.route.snapshot.queryParamMap.get('email');
  }

  irAlPago() {
    if (!this.email) {
      alert("Error: No se ha detectado el email del usuario.");
      return;
    }

    this.paymentService.prepay(this.email).subscribe({
      next: (res: any) => {
        console.log("Respuesta de Stripe recibida:", res);
        alert("¡Éxito! Conectado con el Backend de Stripe.");
      },
      error: (err: any) => {
        console.error("Error al preparar el pago:", err);
        alert("Error al preparar el pago: " + (err.error?.message || "Revisa la consola"));
      }
    });
  }
}