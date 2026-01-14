import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

export const suscripcionGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const userJson = sessionStorage.getItem('user');

  if (userJson) {
    try {
      const user = JSON.parse(userJson);
      // Verificar si el usuario está suscrito
      if (user && user.paid) {
        return true;
      }
    } catch (e) {
      console.error("Error leyendo el usuario de la sesión", e);
    }
  }

  // Usuario no suscrito
  alert('Debes estar suscrito para acceder a esta sección.');

  if (userJson) {
    router.navigate(['/payment']);
  } else {
    router.navigate(['/login']);
  }

  return false;
};