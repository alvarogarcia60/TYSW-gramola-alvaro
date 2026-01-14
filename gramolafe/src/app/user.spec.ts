import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  constructor(private http: HttpClient) { }

  register(bar: string, email: string, pwd1: string, pwd2: string, clientId: string, clientSecret: string) {
    const body = {
      bar: bar,
      email: email,
      pwd1: pwd1,
      pwd2: pwd2,
      clientId: clientId,
      clientSecret: clientSecret
    };
    return this.http.post("http://localhost:8080/users/register", body);
  }
}