import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../user';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule],  
  templateUrl: './register.html',
  styleUrls: ['./register.css']
})
export class RegisterComponent {
  email?: string;
  pwd1?: string;
  pwd2?: string;
  registroOk:boolean = false;
  registroKo:boolean = false;

  msgOk = '';
  msgErr = '';

  constructor(private service: UserService) {}

  registrar() {
    this.msgOk = '';
    this.msgErr = '';
    this.registroOk = false;
    this.registroKo = false;

    if (!this.email || !this.pwd1 || !this.pwd2) {
      this.msgErr = 'Rellena todos los campos';
      return;
    }
    if (this.pwd1 !== this.pwd2) {
      this.msgErr = 'Las contraseñas no coinciden';
      return;
    }

    this.service
      .register(this.email, this.pwd1, this.pwd2)
      .subscribe({
        next: () => (this.msgOk = 'Registro exitoso ✅'),
        error: () => {
          this.msgErr = 'Error en el registro ❌';
          this.registroKo = true;
        },
        complete: () => (this.registroOk = true)
      });
  }
}
