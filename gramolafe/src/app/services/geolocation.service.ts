import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class GeolocationService {
  private baseUrl = 'http://localhost:8080/users';
  private readonly RADIUS_METERS = 100; // Radio permitido en metros

  constructor(private http: HttpClient) {}

  /* Obtiene las coordenadas del bar desde el backend */
  getBarLocation(email: string): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/bar-location/${email}`);
  }

  /* Obtiene la ubicación actual del usuario usando Geolocation API */
  getCurrentLocation(): Promise<{ latitude: number; longitude: number }> {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error('Geolocation no soportado en este navegador'));
        return;
      }

      navigator.geolocation.getCurrentPosition(
        (position) => {
          resolve({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude
          });
        },
        (error) => {
          reject(error);
        },
        {
          enableHighAccuracy: true,
          timeout: 10000,
          maximumAge: 0
        }
      );
    });
  }

  /**
   * Calcula la distancia entre dos puntos usando la fórmula de Haversine
   * @param lat1 Latitud del punto 1
   * @param lon1 Longitud del punto 1
   * @param lat2 Latitud del punto 2
   * @param lon2 Longitud del punto 2
   * @returns Distancia en metros
   */
  calculateDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const R = 6371000; // Radio de la Tierra en metros
    const dLat = this.toRad(lat2 - lat1);
    const dLon = this.toRad(lon2 - lon1);
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(this.toRad(lat1)) * Math.cos(this.toRad(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  /* Verifica si la ubicación actual está dentro del radio permitido */
  async isWithinRadius(barLatitude: number, barLongitude: number): Promise<boolean> {
    try {
      const userLocation = await this.getCurrentLocation();
      const distance = this.calculateDistance(
        userLocation.latitude,
        userLocation.longitude,
        barLatitude,
        barLongitude
      );
      return distance <= this.RADIUS_METERS;
    } catch (error) {
      console.error('Error verificando ubicación:', error);
      throw error;
    }
  }

  /* Convierte grados a radianes */
  private toRad(degrees: number): number {
    return degrees * (Math.PI / 180);
  }
}
