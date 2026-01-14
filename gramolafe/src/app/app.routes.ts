import { Routes } from '@angular/router';
import { WelcomeComponent } from './welcome/welcome';
import { LoginComponent } from './login/login';
import { RegisterComponent } from './register/register';
import { PaymentComponent } from './payment/payment';
import { SearchSongsComponent } from './search-songs/search-songs';
import { MainMenuComponent } from './main-menu/main-menu';
import { MyBarComponent } from './my-bar/my-bar';
import { suscripcionGuard } from './guards/suscripcion.guard';
import { CallbackComponent } from './callback/callback.component';
import { ForgotPasswordComponent } from './forgot-password/forgot-password';
import { ResetPasswordComponent } from './reset-password/reset-password';

export const routes: Routes = [
  { path: '', component: WelcomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'payment', component: PaymentComponent },
  { path: 'callback', component: CallbackComponent },
  { 
    path: 'admin-jukebox', 
    component: SearchSongsComponent, 
    canActivate: [suscripcionGuard] 
  },
  { 
    path: 'jukebox/:barEmail', 
    component: SearchSongsComponent 
  },
  { path: 'main-menu', component: MainMenuComponent, canActivate: [suscripcionGuard] },
  { path: 'my-bar', component: MyBarComponent, canActivate: [suscripcionGuard] },
  { path: '**', redirectTo: '' }
];