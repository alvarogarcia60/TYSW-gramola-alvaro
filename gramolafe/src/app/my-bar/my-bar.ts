import { Component, OnInit, OnDestroy } from '@angular/core';
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
export class MyBarComponent implements OnInit, OnDestroy {
  barName: string = '';
  email: string = '';
  subscriptionStatus: any = null;
  transactions: any[] = [];
  loading: boolean = true;
  precios: any[] = [];
  refreshInterval: any;
  signatureImage: string | null = null;

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
      this.loadSignature();

      // Refresco en tiempo real cada 5 segundos
      this.refreshInterval = setInterval(() => {
        this.loadSubscriptionStatus();
        this.loadTransactions();
      }, 5000);
    } else {
      this.router.navigate(['/login']);
    }
  }

  ngOnDestroy() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
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
    this.http.get<any[]>('http://127.0.0.1:8080/precios/lista').subscribe({
      next: (data) => {
        // Mostrar solo planes de suscripción: excluir CANCION y excluir el precio 0.5
        this.precios = (data || [])
          .filter((p) => {
            const concepto = (p?.concepto || '').toUpperCase();
            const importe = parseFloat(p?.importe || p?.precio || 0);
            return !concepto.includes('CANCION') && 
                   !concepto.includes('CANCIÓN') && 
                   importe !== 0.5;  
          })
          .sort((a, b) => {
            const precioA = parseFloat(a?.importe || a?.precio || 0);
            const precioB = parseFloat(b?.importe || b?.precio || 0);
            return precioA - precioB;
          });  // Ordenar de menor a mayor
      },
      error: (err) => {
        console.error('Error al cargar precios:', err);
        this.precios = [];
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

  formatPaymentDate(timestamp: number): string {
    if (!timestamp || timestamp === 0) {
      return 'N/A';
    }
    const date = new Date(timestamp);
    return date.toLocaleDateString('es-ES', { 
      year: 'numeric', 
      month: '2-digit', 
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  goBack() {
    this.router.navigate(['/main-menu']);
  }

  logout() {
    sessionStorage.clear();
    this.router.navigate(['/login']);
  }

  selectPlan(precio: any) {
    // Redirige al pago con el plan seleccionado
    // El backend extenderá la suscripción desde la fecha de expiración actual (si activa) o desde hoy (si expirada)
    this.router.navigate(['/payment'], {
      queryParams: { email: this.email, planId: precio.id }
    });
  }

  loadSignature() {
    this.userService.getSignature(this.email).subscribe({
      next: (data: any) => {
        if (data.signature) {
          this.signatureImage = 'data:image/png;base64,' + data.signature;
        }
      },
      error: (err) => {
        console.log('Sin firma registrada (opcional)');
      }
    });
  }
}
