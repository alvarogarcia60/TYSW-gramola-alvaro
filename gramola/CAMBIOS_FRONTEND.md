# Cambios Realizados en el Frontend (gramolafe)

## Resumen General
Se han implementado todas las funcionalidades administrativas del frontend Angular para integrar los nuevos endpoints del backend. Los cambios incluyen:
- **Control del Reproductor**: Play/Pause, selección de dispositivo Spotify
- **Sección "Mi Bar"**: Historial de transacciones, estado de suscripción, precios
- **Validaciones de Suscripción**: Advertencias de expiración en tiempo real
- **Mejoras en la UI**: Banners informativos, controles mejorados

---

## 1. Nuevos Servicios

### 1.1 UserService (`gramolafe/src/app/services/user.service.ts`)
**Nuevo archivo creado**

Servicio Angular para gestionar endpoints de usuario:
- `getTransactionHistory(email: string)`: Obtiene historial de pagos de Stripe
- `getSubscriptionStatus(email: string)`: Verifica estado de suscripción y días restantes

```typescript
@Injectable({ providedIn: 'root' })
export class UserService {
  private baseUrl = 'http://localhost:8080/users';
  
  getTransactionHistory(email: string): Observable<any[]>
  getSubscriptionStatus(email: string): Observable<any>
}
```

### 1.2 Actualizaciones en MusicService (`gramolafe/src/app/services/music.ts`)
Añadidos nuevos métodos para control de reproducción:
- `play(email: string)`: Inicia reproducción en Spotify
- `pause(email: string)`: Pausa reproducción en Spotify
- `selectDevice(email: string, deviceId: string)`: Selecciona dispositivo de salida
- `checkSubscription(email: string)`: Verifica estado de suscripción
- `getPlaybackState(email: string)`: Obtiene estado actual de reproducción

---

## 2. Nuevos Componentes

### 2.1 MyBarComponent (`gramolafe/src/app/my-bar/`)
**Componente completo creado** (TypeScript, HTML, CSS)

**Funcionalidades:**
- ✅ Muestra estado de suscripción (activa/expirada) con días restantes
- ✅ Historial de transacciones de Stripe con fecha, concepto y detalles
- ✅ Listado de precios dinámicos desde la base de datos
- ✅ Botón de renovación de suscripción
- ✅ Navegación y cierre de sesión

**Archivos:**
- `my-bar.ts` (130 líneas): Lógica del componente con carga de datos
- `my-bar.html` (68 líneas): Template con cards y tabla de transacciones
- `my-bar.css` (215 líneas): Diseño con gradiente púrpura y responsive

**Ejemplo de uso:**
```typescript
ngOnInit() {
  this.loadSubscriptionStatus();  // Verifica suscripción
  this.loadTransactions();         // Carga historial de pagos
  this.loadPrecios();              // Obtiene precios dinámicos
}
```

---

## 3. Actualizaciones en Componentes Existentes

### 3.1 MainMenuComponent
**Archivos modificados:**
- `main-menu.html`: Añadido card "Mi Bar" en dashboard-grid
- `main-menu.ts`: Añadido método `goToMyBar()` para navegación

**Cambio visual:**
```html
<div class="dash-card" (click)="goToMyBar()">
  <span class="card-icon">🍺</span>
  <h3>Mi Bar</h3>
  <p>Consulta tu suscripción, historial de pagos y precios.</p>
</div>
```

### 3.2 SearchSongsComponent
**Archivos modificados:**
- `search-songs.ts`: Añadidos métodos de control de reproducción y verificación de suscripción
- `search-songs.html`: Actualizado con controles play/pause y banner de advertencia
- `search-songs.css`: Añadido estilo para banner de suscripción con animación

**Nuevas funcionalidades:**
1. **Verificación de Suscripción:**
   ```typescript
   checkSubscription() {
     this.musicService.checkSubscription(this.emailBar).subscribe({
       next: (status: any) => {
         if (!status.isActive) {
           this.subscriptionWarning = "⚠️ Tu suscripción ha expirado";
         } else if (status.daysRemaining <= 7) {
           this.subscriptionWarning = `⚠️ Expira en ${status.daysRemaining} días`;
         }
       }
     });
   }
   ```

2. **Control de Reproducción:**
   ```typescript
   play() { this.musicService.play(this.emailBar).subscribe(...); }
   pause() { this.musicService.pause(this.emailBar).subscribe(...); }
   togglePlayPause() { this.isPaused ? this.play() : this.pause(); }
   ```

3. **Selección de Dispositivo Spotify:**
   ```typescript
   seleccionarDispositivo(event: any) {
     const deviceId = event.target.value;
     this.musicService.selectDevice(this.emailBar, deviceId).subscribe(...);
   }
   ```

**Cambios en UI:**
- Banner rojo animado para advertencias de suscripción
- Botones de play/pause visibles solo para administradores
- Selector de dispositivo Spotify con botón de recarga
- Mensajes toast informativos para acciones del usuario

---

## 4. Configuración de Rutas

### 4.1 app.routes.ts
**Ruta añadida:**
```typescript
{ 
  path: 'my-bar', 
  component: MyBarComponent, 
  canActivate: [suscripcionGuard] 
}
```

La ruta `/my-bar` está protegida con el guard de suscripción, asegurando que solo usuarios con sesión activa puedan acceder.

---

## 5. Mejoras en la Experiencia de Usuario

### 5.1 Banners Informativos
- **Suscripción expirada**: Banner rojo con animación pulse
- **Suscripción próxima a expirar**: Advertencia amarilla con días restantes
- **Confirmación de pago**: Banner verde tras añadir canción

### 5.2 Control de Reproducción
- **Botones play/pause**: Solo visibles para administradores
- **Sincronización en tiempo real**: Estado de reproducción actualizado cada 3 segundos
- **Selector de dispositivo**: Dropdown con dispositivos Spotify disponibles
- **Botón "Siguiente"**: Salta a la siguiente canción de la cola

### 5.3 Sección "Mi Bar"
- **Cards con gradientes**: Diseño moderno con sombras y transiciones
- **Tabla de transacciones**: Historial completo con fecha, concepto y detalles JSON
- **Grid de precios**: Muestra todos los precios del servicio desde BD
- **Botón de renovación**: Redirige a página de pago para renovar suscripción

---

## 6. Tecnologías Utilizadas

- **Angular 20.3.5**: Framework principal con componentes standalone
- **RxJS**: Observables para llamadas HTTP asíncronas
- **HttpClient**: Cliente HTTP para comunicación con backend REST
- **CommonModule**: Directivas @if, @for para renderizado condicional
- **FormsModule**: Binding bidireccional con [(ngModel)]
- **Router**: Navegación entre componentes y guards de autenticación

---

## 7. Endpoints Consumidos

### Backend REST APIs:
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/music/play?email=` | Inicia reproducción |
| POST | `/music/pause?email=` | Pausa reproducción |
| POST | `/music/select-device?email=&deviceId=` | Selecciona dispositivo |
| GET | `/music/check-subscription?email=` | Verifica suscripción |
| GET | `/music/playback-state?email=` | Estado de reproducción |
| GET | `/music/devices?email=` | Lista dispositivos Spotify |
| GET | `/users/transactions/{email}` | Historial de pagos |
| GET | `/users/subscription-status/{email}` | Estado de suscripción |
| GET | `/users/precios/lista` | Lista de precios dinámicos |

---

## 8. Pruebas Recomendadas

### 8.1 Flujo Completo del Administrador
1. Login con email de bar registrado
2. Verificar aparición del card "Mi Bar" en dashboard
3. Acceder a "Mi Bar" y validar:
   - Estado de suscripción correcto
   - Historial de transacciones completo
   - Precios dinámicos desde BD
4. Ir a "La Gramola" y probar:
   - Selección de dispositivo Spotify
   - Play/Pause de reproducción
   - Advertencia si suscripción está por expirar
   - Añadir canción gratis a la cola

### 8.2 Flujo del Cliente
1. Acceder a `/jukebox/{emailBar}`
2. Buscar y añadir canción (redirige a pago)
3. Completar pago con Stripe
4. Verificar banner de confirmación en gramola
5. Ver canción en cola de reproducción

### 8.3 Casos de Error
- Suscripción expirada: Banner rojo + bloqueo de funciones críticas
- Sin dispositivo Spotify: Mensaje de error en selector
- Pago fallido: Redirección a página de error
- Token Spotify inválido: Botón de re-autenticación

---

## 9. Archivos Modificados/Creados

### Nuevos Archivos:
- ✅ `gramolafe/src/app/services/user.service.ts`
- ✅ `gramolafe/src/app/my-bar/my-bar.ts`
- ✅ `gramolafe/src/app/my-bar/my-bar.html`
- ✅ `gramolafe/src/app/my-bar/my-bar.css`

### Archivos Modificados:
- ✅ `gramolafe/src/app/app.routes.ts`
- ✅ `gramolafe/src/app/services/music.ts`
- ✅ `gramolafe/src/app/main-menu/main-menu.ts`
- ✅ `gramolafe/src/app/main-menu/main-menu.html`
- ✅ `gramolafe/src/app/search-songs/search-songs.ts`
- ✅ `gramolafe/src/app/search-songs/search-songs.html`
- ✅ `gramolafe/src/app/search-songs/search-songs.css`

---

## 10. Próximos Pasos

### Para el Desarrollador:
1. ✅ Compilar y probar el frontend: `cd gramolafe && npm install && ng serve`
2. ✅ Verificar backend corriendo en `http://localhost:8080`
3. ✅ Probar flujo completo con cuenta de prueba
4. ⏳ Ejecutar tests unitarios: `ng test`

### Para Producción:
- Cambiar URLs hardcodeadas a variables de entorno
- Configurar CORS en backend para dominio real
- Activar modo producción en Stripe (live keys)
- Configurar build optimizado: `ng build --configuration production`
- Deploy en servidor web (Nginx, Apache, etc.)

---

## Resumen Final

✅ **7 archivos modificados** + **4 archivos nuevos** = **11 cambios totales**  
✅ **Todas las funcionalidades administrativas implementadas**  
✅ **Frontend completamente integrado con backend**  
✅ **UI moderna con diseño responsive**  
✅ **Validaciones de suscripción en tiempo real**  
✅ **Control total de reproducción Spotify**

El frontend está **listo para pruebas** y completamente alineado con los requisitos del profesor.

---

## 11. Gestión de borrado en Gramola (Admin)

### Alcance y comportamiento
- **Borrado en Gramola/BD:** El botón de borrar elimina la canción únicamente de la Gramola y la base de datos; no modifica la cola de Spotify.
- **Auto-limpieza:** Las canciones reproducidas desaparecen automáticamente de la Gramola/BD según avanza la reproducción en Spotify.

### Controles y UX
- **Aviso visible:** En la cabecera de la cola se muestra el aviso “Elimina solo en Gramola (no Spotify)”.
- **Confirmación:** Antes de borrar se solicita confirmación indicando “Spotify no se modifica”.
- **Restricción de rol:** Los botones de borrar y “Limpiar cola” se muestran solo si `isAdmin` es verdadero.

### Endpoints usados (backend)
- `DELETE /music/clear-queue?email=<barEmail>`: Limpieza masiva de la cola del bar (solo admin).
- `DELETE /music/delete-song/{id}?email=<barEmail>`: Elimina una canción por ID en Gramola/BD solo si pertenece a ese bar.

### Cambios realizados
- `search-songs.html`: Aviso admin y botón “🧹 Limpiar cola” con handler; `title` en el botón de borrar.
- `search-songs.ts`: `eliminarCancion(id)` con confirmación y toast; `clearQueue()` con confirmación y llamada al servicio.
- `music.ts`: Añadido `clearQueue(email: string)` que invoca el endpoint `DELETE /music/clear-queue`.
