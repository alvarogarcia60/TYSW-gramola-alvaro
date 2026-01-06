import { Routes } from '@angular/router';
import { WelcomeComponent } from './welcome/welcome';
import { LoginComponent } from './login/login';
import { RegisterComponent } from './register/register';
import { PaymentComponent } from './payment/payment';
import { SearchSongsComponent } from './search-songs/search-songs';
import { MainMenuComponent } from './main-menu/main-menu';
import { suscripcionGuard } from './guards/suscripcion.guard';
// 1. Importamos el nuevo componente de callback
import { CallbackComponent } from './callback/callback.component';

export const routes: Routes = [
  { path: '', component: WelcomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'payment', component: PaymentComponent },
  
  // 2. RUTA CRÍTICA PARA SPOTIFY (Sección 3.1.2)
  // Esta es la URL a la que Spotify te devuelve (http://127.0.0.1:4200/callback)
  { path: 'callback', component: CallbackComponent },
  
  // VISTA DEL DUEÑO (Protegida)
  { 
    path: 'admin-jukebox', 
    component: SearchSongsComponent, 
    canActivate: [suscripcionGuard] 
  },
  
  // VISTA DEL CLIENTE (Pública)
  { 
    path: 'jukebox/:barEmail', 
    component: SearchSongsComponent 
  },
  
  { path: 'main-menu', component: MainMenuComponent, canActivate: [suscripcionGuard] },
  { path: '**', redirectTo: '' }
];