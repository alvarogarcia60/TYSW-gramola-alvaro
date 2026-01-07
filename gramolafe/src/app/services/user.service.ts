import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class UserService {
  private baseUrl = 'http://localhost:8080/users';

  constructor(private http: HttpClient) { }

  getTransactionHistory(email: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/transactions/${email}`);
  }

  getSubscriptionStatus(email: string): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/subscription-status/${email}`);
  }
}
