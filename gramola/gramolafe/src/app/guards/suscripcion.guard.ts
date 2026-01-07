import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

export const suscripcionGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const userJson = sessionStorage.getItem('user');
  
  if (userJson) {
    try {
      const user = JSON.parse(userJson);
      // Verificamos si existe la propiedad paid y es verdadera
      if (user && user.paid) {
        return true; 
      }
    } catch (e) {
      console.error("Error leyendo el usuario de la sesión", e);
    }
  }

  // Si llegamos aquí, el usuario no ha pagado o no existe en sesión
  alert('Debes estar suscrito para acceder a esta sección.');
  
  // Si existe el usuario pero no ha pagado, vamos a pago. Si no, a login.
  if (userJson) {
    router.navigate(['/payment']);
  } else {
    router.navigate(['/login']);
  }
  
  return false;
};