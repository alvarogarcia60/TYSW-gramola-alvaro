import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class UserService {
  private baseUrl = 'http://127.0.0.1:8080/users';

  constructor(private http: HttpClient) { }

  login(email: string, password: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/login`, { email, password });
  }

  register(bar: string, email: string, pwd1: string, pwd2: string, clientId: string, clientSecret: string, address?: string, signature?: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/register`, { bar, email, pwd1, pwd2, clientId, clientSecret, address, signature });
  }

  getTransactionHistory(email: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/transactions/${email}`);
  }

  getSubscriptionStatus(email: string): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/subscription-status/${email}`);
  }

  renewSubscription(email: string, plan: string): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/renew-subscription?email=${email}&plan=${plan}`, {});
  }

  getSignature(email: string): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/signature/${email}`);
  }
}
