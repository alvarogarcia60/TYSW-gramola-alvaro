import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  private url = "http://localhost:8080/users";

  constructor(private http: HttpClient) { }

  register(bar: string, email: string, pwd1: string, pwd2: string, clientId: string, clientSecret: string): Observable<any> {
    const body = { bar, email, pwd1, pwd2, clientId, clientSecret };
    return this.http.post(`${this.url}/register`, body);
  }

  login(email: string, pwd1: string): Observable<any> {
    return this.http.post(`${this.url}/login`, { email, pwd1 });
  }

  getAll(): Observable<any> {
    return this.http.get(`${this.url}/getAll`);
  }

  // Cambiado a 'any' para evitar conflictos entre number y string
  getById(id: any): Observable<any> {
    return this.http.get(`${this.url}/getById/${id}`);
  }

  // Cambiado a 'any' para evitar conflictos entre number y string
  delete(id: any): Observable<any> {
    return this.http.delete(`${this.url}/delete/${id}`);
  }
}