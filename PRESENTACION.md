# Guion de presentación — eTrip Backend

Este archivo **no reemplaza** al [README](README.md) ni a [`insomnia/ENDPOINTS.md`](insomnia/ENDPOINTS.md)
(ese es el detalle técnico completo de cada endpoint). Esto es el **guion para el día de la
demo**: qué mostrar, en qué orden, con qué datos, y quién dice qué. Pensado para armarse con
Insomnia en vivo, proyectando la pantalla.

Somos 4 en el grupo pero puede que solo **3 presenten**. El guion está armado en **3 bloques**
independientes (uno por persona) en vez de estar dividido por "quién programó qué módulo" — así
cualquiera de los 3 puede quedarse con cualquier bloque, sin depender de quién falte.

---

## 0. Setup previo (antes de la clase, no en vivo)

Hacer esto **antes** de presentar para no perder tiempo en vivo:

1. `MySQL80` corriendo, base `etrip_db` accesible.
2. Backend levantado: `./mvnw spring-boot:run` → confirmar que loguea en `http://localhost:4002`.
3. Insomnia: importar `insomnia/eTrip_insomnia.json` y crear/editar el Environment "Local" con:
   ```json
   { "base_url": "http://localhost:4002", "token": "", "user_id": 1 }
   ```
4. **Idealmente arrancar con la base limpia** (o al menos saber qué hay cargado) para que los ids
   que se muestran en pantalla (`categoryId: 1`, `experienceId: 1`, etc.) coincidan con este guion.
   Si la base no está limpia, ajustar los ids sobre la marcha — no es grave, pero hay que decirlo
   antes de arrancar para no perder el hilo en vivo.
5. Crear de antemano un usuario **ADMIN** por SQL (para no perder tiempo en vivo esperando el
   `UPDATE`):
   ```sql
   UPDATE user SET role='ADMIN' WHERE email='admin@etrip.com';
   ```
   (si ese usuario no existe todavía, registrarlo primero como `CLIENTE` con
   `POST /api/v1/auth/register` y después correr el `UPDATE`).
6. Tener a mano, en una pestaña aparte, el diagrama de secuencia del login (`README.md` → sección
   **JWT y autenticación**) por si preguntan cómo viaja el token.

**Usuarios de prueba que usa este guion:**

| Usuario | Email | Rol | Para qué |
|---|---|---|---|
| Ana (anfitriona) | `ana@etrip.com` | CLIENTE | Bloque 1 — publica experiencias |
| Beto (comprador) | `beto@etrip.com` | CLIENTE | Bloque 2 — reserva y reseña |
| Admin | `admin@etrip.com` | ADMIN | Bloque 3 — administración |

---

## Cómo se reparte

| Bloque | Rol que se muestra | Módulos que cubre |
|---|---|---|
| **1** | Anfitrión (publica) | Categorías, Experiencias (+ fotos + descuento), Sesiones |
| **2** | Comprador (reserva) | Búsqueda/filtros, Carrito, Cupones, Checkout, Reservas, Reseñas |
| **3** | Administración | Usuarios/roles, Cupones globales, casos de error / seguridad |

Cada bloque dura ~4-5 minutos si se va directo al grano. Sugerencia: mientras una persona hace los
requests en Insomnia, va explicando en voz alta **qué se está validando**, no solo "clickeo y
sale". Lo que engancha a un profesor no es el 200 OK, es mostrar el 400/403 cuando algo está mal.

---

## Bloque 1 — Anfitrión publica una experiencia

**Carpetas de Insomnia:** `01 - Auth`, `03 - Categorias`, `04 - Experiencias`, `05 - Sesiones`.

1. **`POST /api/v1/auth/register`** — crear a Ana (saltear si ya existe):
   ```json
   { "username": "ana", "firstname": "Ana", "lastname": "Perez", "email": "ana@etrip.com", "password": "pass1234" }
   ```
2. **`POST /api/v1/auth/authenticate`**:
   ```json
   { "usernameOrEmail": "ana@etrip.com", "password": "pass1234" }
   ```
   Copiar `access_token` → pegarlo en la variable `token` del Environment. **Decir en voz alta:**
   el token dura 24hs y adentro solo viaja el email, no el rol (ver README si preguntan).
3. **`POST /experience-categories`**:
   ```json
   { "name": "Tours urbanos", "description": "Recorridos guiados por la ciudad" }
   ```
   *Mostrar que falla:* repetir el mismo `POST` → `400` (nombre duplicado).
4. **`POST /experiences`** (multipart) — partes: `experience` (Text, content-type
   `application/json`) + 1-2 `images` (File):
   ```json
   { "title": "City tour Buenos Aires", "description": "Recorrido por el centro histórico", "price": 25000, "location": "Buenos Aires", "categoryId": 1 }
   ```
   Mostrar en la respuesta `imagesBase64` y `finalPrice` (igual a `price`, todavía sin descuento).
5. **`PATCH /experiences/1/discount`**:
   ```json
   { "discountPercentage": 20 }
   ```
   Volver a pedir `GET /experiences/1` y mostrar que `finalPrice` bajó. *Mostrar que falla:*
   `{ "discountPercentage": 150 }` → `400`.
6. **`POST /experience-sessions`**:
   ```json
   { "experienceId": 1, "startsAt": "2026-10-15T10:00:00", "endsAt": "2026-10-15T13:00:00", "capacity": 5 }
   ```
   *Mostrar que falla:* crear otra sesión con horario superpuesto → `400`.
7. **`GET /experiences/mine`** — Ana ve lo que publicó.

**Cierre del bloque:** "Ana publicó una experiencia con fotos, le puso un descuento propio, y le
cargó un turno con 5 cupos. Ahora Beto la va a encontrar, reservar y pagar con un cupón."

---

## Bloque 2 — Comprador busca, reserva y opina

**Carpetas de Insomnia:** `01 - Auth`, `04 - Experiencias`, `07 - Carrito`, `10 - Cupones`,
`08 - Ordenes`, `09 - Reservas`, `06 - Resenias`.

1. **Registrar/loguear a Beto** (mismo patrón que Ana, `beto@etrip.com`). Actualizar `token` y
   `user_id` en el Environment con el id de Beto.
2. **`GET /experiences?location=Buenos Aires&onlyDiscounted=true`** — mostrar que el filtro
   combinado encuentra la experiencia de Ana. Cambiar a `minPrice=999999` para mostrar una lista
   vacía sin romper nada (200 con página vacía).
3. **`GET /experiences/1`** — el detalle: fotos, descripción, sesiones disponibles.
4. **`POST /carts/items`**:
   ```json
   { "userId": 2, "experienceSessionId": 1, "quantity": 2 }
   ```
   *Mostrar que falla:* pedir `quantity: 999` (más que `availableSeats`) → `400`.
5. **`GET /carts/user/2`** — mostrar `unitPrice`/`subtotal` ya con el descuento de Ana aplicado.
6. **(Bloque 3 adelantado o ya preparado) `POST /discount-coupons`** con el admin — si ese cupón
   ya se creó en el Bloque 3, saltear este paso; si se presenta en otro orden, crearlo acá:
   ```json
   { "code": "DEMO2026", "percentage": 15, "validFrom": "2026-09-01T00:00:00", "validUntil": "2026-12-31T23:59:59" }
   ```
7. **`GET /discount-coupons/validate?code=DEMO2026`** — siempre `200`, `{ valid: true, ... }`.
   *Mostrar que falla:* `?code=NOEXISTE` → `valid: false, reason: "NOT_FOUND"`.
8. **`POST /orders`**:
   ```json
   { "couponCode": "DEMO2026" }
   ```
   Mostrar `subtotal`, `discountAmount`, `total` en la respuesta, y que el carrito de Beto quedó
   vacío (`GET /carts/user/2`).
9. **`GET /bookings`** — el voucher de Beto (`voucherCode: ETRIP-XXXXXXXX`).
10. **`GET /bookings/sales`** (con el token de **Ana**) — vista inversa: Ana ve que Beto le
    reservó, con `buyerName`.
11. **`POST /reviews`** (con Beto):
    ```json
    { "experienceId": 1, "rating": 5, "comment": "Excelente, muy recomendable" }
    ```
    *Mostrar que falla:* repetir el mismo `POST` → `400` (una reseña por usuario/experiencia).

**Cierre del bloque:** "Todo el flujo de compra quedó registrado: la reserva, el voucher, y la
reseña. Y en cada paso hay una regla de negocio que corta el flujo si algo no corresponde."

---

## Bloque 3 — Administración y seguridad

**Carpetas de Insomnia:** `01 - Auth`, `02 - Usuarios y Perfil`, `10 - Cupones`.

1. **`POST /api/v1/auth/authenticate`** con `admin@etrip.com` (ya promovido a `ADMIN` por SQL en
   el setup previo). Actualizar `token`.
2. **`GET /users`** — la lista completa de cuentas. *Mostrar que falla:* repetir con el `token` de
   Beto (CLIENTE) → `403`.
3. **`PATCH /users/2/role?role=ADMIN`** — asignarle ADMIN a Beto. *Mostrar que falla:* el admin
   intentando `PATCH /users/{su-propio-id}/role` → `403` (no te podés autoasignar/autorevocar).
4. **`GET /users/1`** (perfil de Ana, visto por el admin) — mostrar que como ADMIN se ven el email
   y los contadores privados; repetir con el token de Beto para mostrar que a un CLIENTE ajeno le
   aparece la versión pública (sin email).
5. **`POST /discount-coupons`** — crear un cupón vencido a propósito para mostrar el caso de error:
   ```json
   { "code": "VENCIDO", "percentage": 10, "validFrom": "2020-01-01T00:00:00", "validUntil": "2020-01-31T23:59:59" }
   ```
   `GET /discount-coupons/validate?code=VENCIDO` → `valid: false, reason: "EXPIRED"`.
6. **`DELETE /experience-categories/1`** (con la categoría de Ana, que ya tiene una experiencia
   asociada) → `400`, para mostrar que no se puede romper la integridad de datos borrando algo con
   relaciones.
7. Si da el tiempo: pedir cualquier endpoint **sin** header `Authorization` → `403`, y comentar la
   sección del README **"Por qué un 403 y no un 401"** (Spring Security no distingue "no logueado"
   de "no tenés permiso" sin una `AuthenticationEntryPoint` custom).

**Cierre del bloque:** "La seguridad no es solo login/logout: hay reglas por rol, por dueño del
recurso, y protecciones para que un ADMIN no se pueda sacar sus propios permisos por error."

---

## Preguntas esperables (tenerlas preparadas)

- **¿Por qué Order y no solo Booking?** `Order` es el comprobante que agrupa el subtotal/descuento/
  total y los vouchers de una misma compra; `Booking` es cada reserva individual con su voucher.
- **¿Cómo sabe el sistema quién soy en cada request?** El JWT (`Authorization: Bearer`), validado
  por `JwtAuthenticationFilter` en cada llamada — ver diagrama en el README.
- **¿Por qué un solo rol CLIENTE y no comprador/vendedor separados?** Decisión de diseño, estilo
  Airbnb Experiences: cualquier usuario puede publicar y reservar con la misma cuenta.
- **¿Qué pasa si dos personas reservan el último cupo al mismo tiempo?** Se valida
  `availableSeats` en el momento del checkout (no solo al agregar al carrito), pero no hay locking
  pesimista — es una limitación conocida, no se probó con concurrencia real.
- **¿Qué falta?** Ver la sección "Qué falta / decisiones a tener en cuenta" del README (tests
  automatizados, que el dueño no pueda reservar su propia experiencia, recuperación de contraseña,
  CORS, Swagger).

## Checklist final antes de arrancar

- [ ] Backend arriba, `GET /experiences` responde.
- [ ] Environment de Insomnia con `base_url` correcto.
- [ ] Usuario ADMIN ya promovido por SQL.
- [ ] Los 3 bloques probados una vez de punta a punta (aunque sea rápido) el día anterior.
- [ ] Decidido quién arranca si el que iba primero termina antes/después de tiempo.
