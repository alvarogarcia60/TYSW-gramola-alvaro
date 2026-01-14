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
    // Obtener el código de autorización de Spotify desde la URL
    const code = this.route.snapshot.queryParamMap.get('code');
    const userData = sessionStorage.getItem('user');

    if (code && userData) {
      const user = JSON.parse(userData);
      const url = `http://localhost:8080/users/spotifyCallback?code=${code}&email=${user.email}`;

      // Intercambiar el código por el token de Spotify
      this.http.get<any>(url).subscribe({
        next: (response) => {
          if (response && response.success) {
            user.spotiSimpleToken = 'authorized';
            sessionStorage.setItem('user', JSON.stringify(user));
          }
          this.router.navigate(['/main-menu']);
        },
        error: () => {
          this.router.navigate(['/main-menu']);
        }
      });
    }
  }
}