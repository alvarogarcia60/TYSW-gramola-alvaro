import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../user';
import { Router } from '@angular/router';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class RegisterComponent {
  // Variables vinculadas al formulario
  email = ''; 
  pwd1 = ''; 
  pwd2 = '';
  bar = '';          // Nuevo: necesario para tu UserController
  clientId = '';     // Nuevo: necesario para tu UserController
  clientSecret = ''; // Nuevo: necesario para tu UserController

  // Variables de estado
  msgOk = ''; 
  msgErr = '';
  registroOk = false; 
  registroKo = false;

  constructor(private users: UserService, private router: Router) {}

  registrar() {
    this.msgOk = this.msgErr = '';
    this.registroOk = this.registroKo = false;

    // Validación básica de contraseñas
    if (this.pwd1 !== this.pwd2) { 
      this.msgErr = 'Las contraseñas no coinciden'; 
      return; 
    }

    // Llamada al servicio enviando todos los datos que pide el Backend
    // Asegúrate de que tu UserService.register acepte estos nuevos parámetros
    this.users.register(this.bar, this.email, this.pwd1, this.pwd2, this.clientId, this.clientSecret).subscribe({
      next: () => { 
        this.registroOk = true; 
        this.msgOk = 'Registro OK. Revisa el link en la terminal de Java.'; 
      },
      error: (e) => { 
        this.registroKo = true; 
        this.msgErr = e?.error?.message || 'Error en el registro'; 
      }
    });
  }

  // Método para volver al login (soluciona el error que tenías)
  goToLogin() {
    this.router.navigate(['/login']);
  }
}