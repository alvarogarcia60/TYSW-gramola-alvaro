import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-main-menu',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './main-menu.html',
  styleUrl: './main-menu.css'
})
export class MainMenuComponent implements OnInit, OnDestroy {
  barName: string = "Mi Bar";
  emailBar: string = "";
  
  cancionActual: any = null;
  progresoMS: number = 0;
  duracionMS: number = 0;
  intervaloSincro: any;

  constructor(public router: Router, private http: HttpClient) {}

ngOnInit() {
  const userData = sessionStorage.getItem("user");
  if (userData) {
    const user = JSON.parse(userData);
    this.emailBar = user.email;

    // SOLO sincronizar si el usuario REALMENTE tiene un token de Spotify autorizado
    // No intentar conectar automáticamente para usuarios sin Spotify
    if (user.spotiSimpleToken && user.spotiSimpleToken === "authorized") {
      this.sincronizarSpotify();
      this.intervaloSincro = setInterval(() => this.sincronizarSpotify(), 3000);
    } else {
      console.log("⚠️ Usuario sin autenticación Spotify - funcionalidad Spotify deshabilitada");
    }
  }
}

  /**
   * Redirige al backend para iniciar el Flujo 1 de OAuth 2.0 (Figura 19)
   */
  loginWithSpotify() {
    console.log("Iniciando Flujo 1: Solicitud de permisos a Spotify...");
    // Redirigimos al endpoint que creamos en el UserController del Backend
    window.location.href = `http://localhost:8080/users/loginSpotify?email=${this.emailBar}`;
  }

  ngOnDestroy() {
    if (this.intervaloSincro) clearInterval(this.intervaloSincro);
  }

  sincronizarSpotify() {
    this.http.get<any>(`http://localhost:8080/music/playback-state?email=${this.emailBar}`).subscribe({
      next: (estado) => {
        if (estado && estado.item) {
          this.cancionActual = estado.item;
          this.progresoMS = estado.progress_ms;
          this.duracionMS = estado.item.duration_ms;
        }
      },
      error: () => console.log("Esperando reproducción activa en Spotify o re-autorización...")
    });
  }

  get porcentaje(): number {
    return this.duracionMS > 0 ? (this.progresoMS / this.duracionMS) * 100 : 0;
  }

  goToGramola() { this.router.navigate(['/admin-jukebox']); }
  
  goToAccount() {
    this.router.navigate(['/payment'], { queryParams: { email: this.emailBar } });
  }

  logout() {
    sessionStorage.clear();
    this.router.navigate(['/login']);
  }
}