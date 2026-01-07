import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-callback',
  standalone: true,
  template: '<div style="color: white; text-align: center; margin-top: 50px;"><h2>Sincronizando con Spotify, espera un momento...</h2></div>'
})
export class CallbackComponent implements OnInit {
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    // 1. Capturamos el code que Spotify ha puesto en la URL tras darle a "Aceptar"
    const code = this.route.snapshot.queryParamMap.get('code');
    const userData = sessionStorage.getItem("user");

    if (code && userData) {
      const user = JSON.parse(userData);
      
      // 2. Enviamos el code al Backend (UserController.java) para el intercambio real
      // Usamos el email del usuario logueado para saber a quién guardar el token
      const url = `http://localhost:8080/users/spotifyCallback?code=${code}&email=${user.email}`;
      
      this.http.get<any>(url).subscribe({
        next: (response) => {
          // 3. SOLO marcar como autorizado si el backend confirma éxito
          if (response && response.success) {
            user.spotiSimpleToken = "authorized"; // token real guardado
            sessionStorage.setItem("user", JSON.stringify(user));
            console.log("✅ Token de Spotify sincronizado exitosamente");
          } else {
            console.warn("⚠️ Backend no confirmó la sincronización");
            // No guardar token falso si el backend dice que falló
          }
          
          // 4. Volvemos al Panel de Gestión
          this.router.navigate(['/main-menu']);
        },
        error: (err) => {
          console.error("❌ Error al sincronizar el token", err);
          // Ir a main-menu incluso si falla
          this.router.navigate(['/main-menu']);
        }
      });
    }
  }
}