import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { UserService } from '../services/user.service';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-my-bar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './my-bar.html',
  styleUrl: './my-bar.css'
})
export class MyBarComponent implements OnInit {
  barName: string = '';
  email: string = '';
  subscriptionStatus: any = null;
  transactions: any[] = [];
  loading: boolean = true;
  precios: any[] = [];

  constructor(
    private userService: UserService,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit() {
    const userData = sessionStorage.getItem('user');
    if (userData) {
      const user = JSON.parse(userData);
      this.email = user.email;
      this.barName = user.bar || 'Mi Bar';
      
      this.loadSubscriptionStatus();
      this.loadTransactions();
      this.loadPrecios();
    } else {
      this.router.navigate(['/login']);
    }
  }

  loadSubscriptionStatus() {
    this.userService.getSubscriptionStatus(this.email).subscribe({
      next: (data) => {
        this.subscriptionStatus = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al cargar estado de suscripción:', err);
        this.loading = false;
      }
    });
  }

  loadTransactions() {
    this.userService.getTransactionHistory(this.email).subscribe({
      next: (data) => {
        this.transactions = data;
      },
      error: (err) => {
        console.error('Error al cargar transacciones:', err);
      }
    });
  }

  loadPrecios() {
    this.http.get<any[]>('http://localhost:8080/precios/lista').subscribe({
      next: (data) => {
        // Mostrar solo planes de suscripción para el dueño (oculta precio por canción)
        this.precios = (data || []).filter(p => (p?.id || '').includes('SUB'));
      },
      error: (err) => {
        console.error('Error al cargar precios:', err);
      }
    });
  }

  get daysRemaining(): number {
    return this.subscriptionStatus?.daysRemaining || 0;
  }

  get isActive(): boolean {
    return this.subscriptionStatus?.active || false;
  }

  get expirationDateFormatted(): string {
    if (!this.subscriptionStatus?.expirationDate || this.subscriptionStatus.expirationDate === 0) {
      return 'Sin suscripción';
    }
    const date = new Date(this.subscriptionStatus.expirationDate);
    return date.toLocaleDateString('es-ES', { 
      year: 'numeric', 
      month: 'long', 
      day: 'numeric' 
    });
  }

  getPrecioName(concepto: string): string {
    const precio = this.precios.find(p => p.concepto === concepto);
    return precio ? `${precio.precio} ${precio.moneda}` : 'N/A';
  }

  parseTransactionData(data: string): any {
    try {
      return JSON.parse(data);
    } catch {
      return { raw: data };
    }
  }

  renewSubscription() {
    this.router.navigate(['/payment'], { queryParams: { email: this.email } });
  }

  goBack() {
    this.router.navigate(['/main-menu']);
  }

  logout() {
    sessionStorage.clear();
    this.router.navigate(['/login']);
  }

  selectPlan(precio: any) {
    this.router.navigate(['/payment'], {
      queryParams: { email: this.email, planId: precio.id }
    });
  }
}
