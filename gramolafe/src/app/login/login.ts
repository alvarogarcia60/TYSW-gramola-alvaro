import { Component } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../services/user.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, NgIf, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginComponent {
  email = '';
  password = '';
  ok = '';
  err = '';

  constructor(private users: UserService, private router: Router) {}

  doLogin() {
    this.err = '';
    this.ok = '';
    const emailLogin = this.email.trim();
    const passLogin = this.password.trim();

    if (!emailLogin || !passLogin) {
      this.err = 'Por favor, introduce email y contraseña';
      return;
    }

    this.users.login(emailLogin, passLogin).subscribe({
      next: (user: any) => {
        this.ok = '¡Acceso concedido! ✅';
        sessionStorage.setItem('user', JSON.stringify(user));
        sessionStorage.setItem('userEmail', user.email);
        sessionStorage.setItem('emailLogeado', user.email);
        setTimeout(() => this.router.navigate(['/main-menu']), 1000);
      },
      error: e => {
        this.err = e?.error?.message || 'Error: Credenciales inválidas o cuenta sin activar ❌';
      }
    });
  }

  goToRegister() {
    this.router.navigate(['/register']);
  }

  goToForgotPassword() {
    this.router.navigate(['/forgot-password']);
  }
}