import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.css'
})
export class ResetPasswordComponent implements OnInit {
  email: string = '';
  token: string = '';
  newPassword: string = '';
  confirmPassword: string = '';
  isLoading: boolean = false;
  successMessage: string = '';
  errorMessage: string = '';
  passwordsMatch: boolean = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit() {
    // Obtener email y token de los parámetros de la URL
    this.route.queryParams.subscribe((params) => {
      this.email = params['email'] || '';
      this.token = params['token'] || '';

      if (!this.email || !this.token) {
        this.errorMessage = 'Enlace inválido o expirado';
      }
    });
  }

  onPasswordChange() {
    this.passwordsMatch = this.newPassword === this.confirmPassword;
  }

  onSubmit() {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.newPassword || !this.confirmPassword) {
      this.errorMessage = 'Por favor completa ambos campos de contraseña';
      return;
    }

    if (this.newPassword.length < 6) {
      this.errorMessage = 'La contraseña debe tener al menos 6 caracteres';
      return;
    }

    if (!this.passwordsMatch) {
      this.errorMessage = 'Las contraseñas no coinciden';
      return;
    }

    this.isLoading = true;

    const body = {
      email: this.email,
      token: this.token,
      newPassword: this.newPassword
    };

    this.http.post<any>('http://localhost:8080/users/resetPassword', body).subscribe({
      next: (response) => {
        this.isLoading = false;
        if (response.success === 'true' || response.success === true) {
          this.successMessage = '✅ Contraseña cambiada exitosamente. Redirigiendo al login...';
          setTimeout(() => {
            this.router.navigate(['/login']);
          }, 2000);
        } else {
          this.errorMessage = response.message || 'Error al cambiar la contraseña';
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = 'Token inválido o expirado. Solicita un nuevo enlace de recuperación.';
        console.error('Error:', err);
      }
    });
  }

  goToForgotPassword() {
    this.router.navigate(['/forgot-password']);
  }
}
