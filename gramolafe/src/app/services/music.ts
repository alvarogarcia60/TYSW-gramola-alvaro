import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class MusicService {
  private baseUrl = 'http://localhost:8080/music';

  constructor(private http: HttpClient) { }

  search(texto: string, email: string): Observable<any[]> {
    const url = `${this.baseUrl}/search?texto=${texto}&email=${email}`;
    return this.http.get<any[]>(url);
  }

  addSong(song: any, email: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/add?email=${email}`, song);
  }

  getPlaylist(email: string): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/getPlaylist?email=${email}`);
  }

  toggleReproduccion(email: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/toggle?email=${email}`, {});
  }

  deleteSong(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/delete-song/${id}`);
  }

  // --- NUEVOS MÉTODOS PARA SECCIÓN 4.1 ---
  getDevices(email: string): Observable<any> {
    return this.http.get(`${this.baseUrl}/devices?email=${email}`);
  }

  setDevice(email: string, deviceId: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/set-device?email=${email}&deviceId=${deviceId}`, {});
  }
}