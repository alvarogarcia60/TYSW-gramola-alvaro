# La Gramola – Entrega Final

Proyecto de jukebox para bares con pagos, geolocalización y firma digital.

## Resumen
- Backend: Spring Boot 3.3.4 (JDK 21), MySQL.
- Frontend: Angular (proyecto `gramolafe`), servidor en 127.0.0.1:4200.
- Pagos Stripe integrados y validados con pruebas E2E.
- Bonus implementados:
  - Geolocalización: conversión de dirección → coordenadas (Nominatim) y verificación de radio 100 m (Haversine).
  - Firma digital: captura en Canvas, almacenamiento en BD (BLOB) y visualización en panel del bar.

## Repositorio
- Código: https://github.com/alvarogarcia60/TYSW-gramola-alvaro
- Tag de entrega: `v1.0.0` (Entrega final La Gramola)

## Configuración
Editar `src/main/resources/application.properties`:
- `spring.datasource.url=jdbc:mysql://localhost:3306/gramola?useSSL=false&serverTimezone=UTC`
- `spring.datasource.username=tu_usuario`
- `spring.datasource.password=tu_password`
- `stripe.secret=sk_test_xxx` (clave secreta de Stripe; soporta test cards)

Notas:
- Nominatim requiere cabecera `User-Agent` en llamadas (ya añadido en backend).
- Frontend y backend usan `127.0.0.1` para evitar problemas con Stripe/Spotify.

## Puesta en marcha
### Backend
- Windows:
  - `./mvnw.cmd spring-boot:run`
  - Alternativa: `./mvnw.cmd clean package -DskipTests` y luego `java -jar target/gramola-0.0.1-SNAPSHOT.war`
- Arranca en `http://127.0.0.1:8080`.

### Frontend (cliente)
- En el proyecto Angular (`gramolafe`):
  - `npm install`
  - `ng serve --host 127.0.0.1 --port 4200`
- Navegar a `http://127.0.0.1:4200`.

## Flujo del Bar (Admin)
- Login del bar y acceso a `admin-jukebox` o `my-bar`.
- Seleccionar dispositivo de salida (Spotify) y controlar reproducción/cola.
- Panel `my-bar`: muestra firma digital y estado de suscripción.

## Flujo del Cliente
1. Abrir `http://127.0.0.1:4200/jukebox/{emailDelBar}`.
2. Buscar canción en el buscador.
3. Añadir canción:
   - Si eres cliente: se abre pago Stripe.
   - Tras pago válido: la canción se cuela en posición 2 de la cola.
4. Geolocalización:
   - Si estás a >100 m del bar, aparece aviso “⚠️ Estás a Xm del bar…”.
   - Acepta permisos de ubicación del navegador.

## Geolocalización
- Registro del bar con dirección: ejemplo “Plaza Mayor, Madrid ”.
- El backend convierte a coordenadas vía Nominatim y guarda `latitude`/`longitude`.
- Verificación en cliente: cálculo Haversine (100 m) y aviso si fuera de rango.
- Comprobar BD:
  - `SELECT email, bar, address, latitude, longitude FROM users;`
- Endpoint de comprobación:
  - `GET http://127.0.0.1:8080/users/bar-location/{email}`

## Pruebas de Pago (Stripe)
- Tarjeta OK: `4242 4242 4242 4242` (cualquier fecha/CVC válidos)
- Tarjeta fallo: `4000 0000 0000 0002`
- Tras el éxito se valida PaymentIntent con metadata `email` y se añade a cola.

## Pruebas E2E (Selenium)
- Ejecutar:
  - `./mvnw.cmd test -Dtest=PaymentE2ETests`
- Casos incluidos:
  - Flujo exitoso: búsqueda, pago, inserción en cola.
  - Flujo de error: tarjeta inválida, no inserta.

## Solución de problemas
- Si no aparece el aviso de distancia:
  - Verifica permisos de geolocalización en el navegador.
  - Comprueba que el bar tiene `latitude`/`longitude` en la BD.
- Si Stripe falla:
  - Asegura que `stripe.secret` esté configurado.
  - Usa `127.0.0.1` en URLs del frontend.

## Archivos clave (backend)
- Controlador usuarios: `src/main/java/edu/uclm/es/gramola/http/UserController.java`
- Servicio usuarios: `src/main/java/edu/uclm/es/gramola/services/UserService.java`
- Servicio música: `src/main/java/edu/uclm/es/gramola/services/MusicService.java`

