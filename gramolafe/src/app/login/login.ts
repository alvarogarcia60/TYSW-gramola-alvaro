import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../user';
import { Router } from '@angular/router'; // 1. Importamos el Router

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginComponent {
  email = '';
  password = '';
  ok = '';
  err = '';

  constructor(private users: UserService, private router: Router) {} // 2. Inyectamos el Router

  doLogin() {
    this.ok = this.err = '';
    if (!this.email || !this.password) { this.err = 'Faltan campos'; return; }
    this.users.login(this.email, this.password).subscribe({
      next: r => this.ok = 'Login correcto ✅',
      error: e => this.err = e?.error?.message || 'Login fallido ❌'
    });
  }

  // 3. Método para ir al registro
  goToRegister() {
    this.router.navigate(['/register']);
  }
}