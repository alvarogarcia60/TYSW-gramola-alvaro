import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private url = "http://localhost:8080/payments";

  constructor(private http: HttpClient) { }

  prepay(email: string): Observable<any> {
    // Configuramos el parámetro para el @RequestParam del Backend
    const params = new HttpParams().set('email', email);
    
    // Es importante que sea un POST y que pasemos los params en las opciones
    return this.http.post(`${this.url}/prepay`, null, { params });
  }
}