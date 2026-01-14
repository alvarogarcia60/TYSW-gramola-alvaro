import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  private url = "http://127.0.0.1:8080/users";

  constructor(private http: HttpClient) { }

  register(bar: string, email: string, pwd1: string, pwd2: string, clientId: string, clientSecret: string): Observable<any> {
    const body = { bar, email, pwd1, pwd2, clientId, clientSecret };
    return this.http.post(`${this.url}/register`, body);
  }

  login(email: string, password: string): Observable<any> {
    const loginUrl = "http://127.0.0.1:8080/users/login";
    
    const datos = {
      email: email,
      password: password
    };

    console.log("Servicio Angular enviando a 127.0.0.1:", datos);
    return this.http.post(loginUrl, datos);
  }

  getAll(): Observable<any> {
    return this.http.get(`${this.url}/getAll`);
  }

  getById(id: any): Observable<any> {
    return this.http.get(`${this.url}/getById/${id}`);
  }
  
  delete(id: any): Observable<any> {
    return this.http.delete(`${this.url}/delete/${id}`);
  }
}