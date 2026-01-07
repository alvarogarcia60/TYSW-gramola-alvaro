import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.css'
})
export class ForgotPasswordComponent {
  email: string = '';
  isLoading: boolean = false;
  successMessage: string = '';
  errorMessage: string = '';

  constructor(private http: HttpClient, private router: Router) {}

  onSubmit() {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.email || !this.email.includes('@')) {
      this.errorMessage = 'Por favor ingresa un email válido';
      return;
    }

    this.isLoading = true;

    const body = { email: this.email };

    this.http.post<any>('http://localhost:8080/users/forgotPassword', body).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.successMessage = response.message || 'Si el email existe, recibirás un enlace de recuperación';
        this.email = '';
        // Redirigir al login después de 3 segundos
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 3000);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = 'Error al procesar la solicitud. Intenta nuevamente.';
        console.error('Error:', err);
      }
    });
  }

  goBack() {
    this.router.navigate(['/login']);
  }
}
