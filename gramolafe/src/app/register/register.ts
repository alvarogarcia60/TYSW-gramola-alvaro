import { Component, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../services/user.service';
import { Router } from '@angular/router';
import SignaturePad from 'signature_pad';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class RegisterComponent implements AfterViewInit {
  // Variables vinculadas al formulario
  email = ''; 
  pwd1 = ''; 
  pwd2 = '';
  bar = '';
  address = '';
  clientId = '';
  clientSecret = '';
  signature = '';
  signatureMessage = '';

  // Signature Pad
  @ViewChild('signatureCanvas') signatureCanvas!: ElementRef<HTMLCanvasElement>;
  signaturePad!: SignaturePad;

  // Variables de estado
  msgOk = ''; 
  msgErr = '';
  registroOk = false; 
  registroKo = false;

  constructor(private users: UserService, private router: Router) {}

  ngAfterViewInit() {
    this.initializeSignaturePad();
  }

  // Inicializar SignaturePad
  private initializeSignaturePad() {
    const canvas = this.signatureCanvas.nativeElement;
    this.signaturePad = new SignaturePad(canvas, {
      minWidth: 0.5,
      maxWidth: 2.5,
      penColor: '#fff',
      backgroundColor: 'rgba(0, 0, 0, 0.5)'
    });

    // Ajustar tamaño del canvas
    this.resizeCanvas();
    window.addEventListener('resize', () => this.resizeCanvas());
  }

  // Ajustar tamaño del canvas
  private resizeCanvas() {
    const canvas = this.signatureCanvas.nativeElement;
    const rect = canvas.parentElement?.getBoundingClientRect();
    if (rect) {
      canvas.width = rect.width;
      canvas.height = 200;
    }
  }

  // Limpiar firma
  clearSignature() {
    this.signaturePad.clear();
    this.signature = '';
    this.signatureMessage = '';
  }

  // Guardar firma como Base64
  saveSignature() {
    if (this.signaturePad.isEmpty()) {
      this.signatureMessage = '⚠️ Por favor, firma antes de guardar';
      return;
    }
    const dataUrl = this.signaturePad.toDataURL('image/png');
    // Extrae solo la parte Base64 (después de la coma)
    this.signature = dataUrl.split(',')[1];
    this.signatureMessage = '✅ Firma guardada correctamente';
  }

  registrar() {
    this.msgOk = this.msgErr = '';
    this.registroOk = this.registroKo = false;

    // Validación básica de contraseñas
    if (this.pwd1 !== this.pwd2) { 
      this.msgErr = 'Las contraseñas no coinciden'; 
      return; 
    }

    // Validar que haya firma
    if (!this.signature) {
      this.msgErr = 'Debes firmar el contrato para registrar el bar (1 punto bonus)';
      return;
    }

    // Enviar datos de registro al backend
    this.users.register(this.bar, this.email, this.pwd1, this.pwd2, this.clientId, this.clientSecret, this.address, this.signature).subscribe({
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

  goToLogin() {
    this.router.navigate(['/login']);
  }
}