# Nuevos Endpoints Backend - Funcionalidades Administrativas

## ✅ Implementado en el Backend

### 1. Control del Reproductor (MusicController)

#### **POST** `/music/play?email={email}`
Inicia la reproducción en Spotify.
- **Respuesta exitosa**: `{"success": "true", "message": "Reproducción iniciada"}`
- **Respuesta error**: `{"success": "false", "message": "Error al iniciar reproducción"}`

#### **POST** `/music/pause?email={email}`
Pausa la reproducción en Spotify.
- **Respuesta exitosa**: `{"success": "true", "message": "Reproducción pausada"}`
- **Respuesta error**: `{"success": "false", "message": "Error al pausar reproducción"}`

#### **POST** `/music/select-device?email={email}&deviceId={deviceId}`
Selecciona el dispositivo Spotify donde reproducir la música.
- **Parámetros**: 
  - `email`: Email del propietario
  - `deviceId`: ID del dispositivo Spotify
- **Respuesta exitosa**: `{"success": "true", "message": "Dispositivo seleccionado"}`

#### **GET** `/music/devices?email={email}`
Obtiene la lista de dispositivos Spotify activos del usuario.
- **Respuesta**: Lista de dispositivos con sus propiedades (id, name, type, is_active, etc.)

#### **GET** `/music/check-subscription?email={email}`
Verifica el estado de la suscripción del usuario.
- **Respuesta**:
```json
{
  "active": true,
  "expirationDate": 1735862400000,
  "daysRemaining": 25,
  "isPaid": true
}
```

---

### 2. Gestión de Usuario (UserController)

#### **GET** `/users/transactions/{email}`
Obtiene el historial de transacciones de pago del usuario.
- **Respuesta**: Lista de objetos `StripeTransaction`:
```json
[
  {
    "id": "pi_xxxxx",
    "email": "user@example.com",
    "data": "{ detalles de la transacción }"
  }
]
```

#### **GET** `/users/subscription-status/{email}`
Obtiene el estado detallado de la suscripción.
- **Respuesta**:
```json
{
  "email": "user@example.com",
  "bar": "Mi Bar",
  "isPaid": true,
  "active": true,
  "expirationDate": 1735862400000,
  "daysRemaining": 25
}
```

---

### 3. Validaciones de Suscripción

#### **GET** `/music/getPlaylist?email={email}`
**MODIFICADO**: Ahora valida suscripción activa antes de devolver la playlist.
- **Si suscripción expirada** (HTTP 403):
```json
{
  "error": "Suscripción expirada",
  "message": "Tu suscripción ha expirado. Por favor renueva tu suscripción para continuar."
}
```

#### **POST** `/music/add`
**MODIFICADO**: Ahora valida suscripción activa antes de permitir agregar canciones gratis.
- **Si suscripción expirada** (HTTP 403):
```json
{
  "error": "Suscripción expirada",
  "message": "La suscripción del bar ha expirado. El propietario debe renovarla."
}
```

---

### 4. Precios desde Base de Datos

#### **GET** `/precios/lista`
Obtiene todos los precios configurados en la base de datos.
- **Respuesta**:
```json
[
  {
    "id": 1,
    "concepto": "Canción adelantada",
    "precio": 0.50,
    "moneda": "EUR"
  },
  {
    "id": 2,
    "concepto": "Suscripción mensual",
    "precio": 9.99,
    "moneda": "EUR"
  }
]
```

---

## 📋 Tareas Pendientes en el Frontend

### Panel de Gestión (Owner)
1. **Reproductor**:
   - Botón "Seleccionar Dispositivo" → Llamar `GET /music/devices` y mostrar lista
   - Al seleccionar dispositivo → Llamar `POST /music/select-device`
   - Botón "Play/Pause" → Alternar entre `POST /music/play` y `POST /music/pause`
   - Botón "Borrar canción" → Ya existe (`DELETE /music/delete-song/{id}`)

2. **Mi Bar**:
   - Mostrar estado de suscripción → Llamar `GET /users/subscription-status/{email}`
   - Mostrar historial de pagos → Llamar `GET /users/transactions/{email}`
   - Botón "Cerrar Sesión" → Limpiar `sessionStorage` y redirigir al login

3. **Validaciones**:
   - Verificar suscripción antes de acceder al reproductor → `GET /music/check-subscription`
   - Si `active: false`, mostrar alerta y botón para renovar suscripción

### Jukebox (Cliente)
1. **Precios Dinámicos**:
   - Al cargar la vista, llamar `GET /precios/lista`
   - Buscar el concepto "Canción adelantada" y usar su precio
   - Mostrar el precio en la interfaz (Ej: "0.50 EUR")

2. **Mensajes de Error**:
   - Si `POST /music/add` devuelve HTTP 403 → Mostrar alerta con el mensaje de error
   - Si usuario busca sin token Spotify → Mostrar "Debes sincronizar con Spotify primero"

---

## 🧪 Testing

Para probar los nuevos endpoints, puedes usar:

```bash
# Verificar suscripción
curl http://localhost:8080/music/check-subscription?email=algarcimartinez@gmail.com

# Obtener dispositivos
curl http://localhost:8080/music/devices?email=algarcimartinez@gmail.com

# Pausar/Reanudar
curl -X POST http://localhost:8080/music/pause?email=algarcimartinez@gmail.com
curl -X POST http://localhost:8080/music/play?email=algarcimartinez@gmail.com

# Historial de transacciones
curl http://localhost:8080/users/transactions/algarcimartinez@gmail.com

# Estado de suscripción
curl http://localhost:8080/users/subscription-status/algarcimartinez@gmail.com

# Lista de precios
curl http://localhost:8080/precios/lista
```

---

## 💡 Notas Importantes

1. **Expiración de Suscripción**: El sistema calcula automáticamente si la suscripción está activa comparando `expirationDate` con la fecha actual.

2. **Días Restantes**: Se calcula como `(expirationDate - now) / (1000 * 60 * 60 * 24)`.

3. **Estados de Reproducción**: El campo `playing` en la tabla `user` se actualiza automáticamente al llamar play/pause.

4. **CORS**: Todos los endpoints permiten peticiones desde `http://localhost:4200` y `http://127.0.0.1:4200`.

5. **Spotify Token**: Todos los endpoints de reproducción requieren que el usuario tenga un token de Spotify válido (`spotiSimpleToken`).
