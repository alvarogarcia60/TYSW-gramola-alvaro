import { Routes } from '@angular/router';
import { WelcomeComponent } from './welcome/welcome';
import { LoginComponent } from './login/login';
import { RegisterComponent } from './register/register';
import { PaymentComponent } from './payment/payment';

export const routes: Routes = [
  { path: '', component: WelcomeComponent },       // Página de inicio (Welcome)
  { path: 'login', component: LoginComponent },     // Pantalla de login
  { path: 'register', component: RegisterComponent }, // Pantalla de registro
  { path: 'payment', component: PaymentComponent },   // Pantalla de Stripe
  { path: '**', redirectTo: '' }                    // Si el usuario se pierde, al Welcome
];