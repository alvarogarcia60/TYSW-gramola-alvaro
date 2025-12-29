import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class UserService {
  // Si usas proxy, usa '/users' en lugar de 'http://localhost:8080/users'
  private baseUrl = 'http://localhost:8080/users';

  constructor(private http: HttpClient) {}

  // ya tenías:
  register(email: string, pwd1: string, pwd2: string) {
    return this.http.post<any>(`${this.baseUrl}/register`, { email, pwd1, pwd2 });
  }

  // NUEVOS ACCESOS
  login(email: string, password: string) {
    return this.http.post<any>(`${this.baseUrl}/login`, { email, password });
  }

  getAll() {
    return this.http.get<any[]>(`${this.baseUrl}`);
  }

  getById(id: number) {
    return this.http.get<any>(`${this.baseUrl}/${id}`);
  }

  update(user: any) {
    // Ajusta el esquema según tu backend
    return this.http.put<any>(`${this.baseUrl}/${user.id}`, user);
  }

  delete(id: number) {
    return this.http.delete<any>(`${this.baseUrl}/${id}`);
  }
}
