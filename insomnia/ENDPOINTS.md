# eTrip - Endpoints para Insomnia

**Base URL:** `http://localhost:4002` (sin context-path)
**Puerto:** 4002 · **DB:** MySQL `etrip_db`

## Cómo usar

1. Insomnia → *Import* → `eTrip_insomnia.json` (ya trae carpetas + environment "Local").
2. Correr **POST authenticate**, copiar el valor de `access_token` de la respuesta.
3. Pegarlo en el environment (`Manage Environments` → `token`).
4. Todas las requests menos `/api/v1/auth/**` mandan `Authorization: Bearer {{ token }}`.

## Reglas de seguridad

| Recurso | Permisos |
|---|---|
| `/api/v1/auth/**` | público |
| `/experiences`, `/experience-categories`, `/experience-sessions` | CLIENTE o ADMIN |
| `/reviews`, `/carts`, `/orders`, `/bookings` | CLIENTE o ADMIN |
| `GET /discount-coupons/**` | CLIENTE o ADMIN |
| `POST/PUT/DELETE /discount-coupons/**` | solo ADMIN |

- **`register` siempre crea rol `CLIENTE`** (el campo `role` del body se ignora).
- Para tener un ADMIN: `UPDATE user SET role='ADMIN' WHERE email='...';` en MySQL y volver a hacer `authenticate` para obtener un token nuevo.
- Fechas: formato ISO local sin zona → `2026-10-15T10:00:00`.

---

## 1. Auth — `/api/v1/auth` (público)

### POST `/api/v1/auth/register`
```json
{
  "firstname": "Nacho",
  "lastname": "Cortes",
  "email": "nacho@etrip.com",
  "password": "1234",
  "role": "CLIENTE"
}
```

### POST `/api/v1/auth/authenticate`
```json
{
  "email": "nacho@etrip.com",
  "password": "1234"
}
```
Respuesta: `{ "access_token": "eyJ..." }`

---

## 2. Categorías — `/experience-categories`

| Método | Ruta | Body |
|---|---|---|
| GET | `/experience-categories?page=0&size=10` | — |
| GET | `/experience-categories/{categoryId}` | — |
| POST | `/experience-categories` | ver abajo |
| PUT | `/experience-categories/{categoryId}` | ver abajo |
| DELETE | `/experience-categories/{categoryId}` | — |

### POST / PUT
```json
{
  "name": "Tours urbanos",
  "description": "Recorridos guiados por la ciudad"
}
```

---

## 3. Experiencias — `/experiences`

| Método | Ruta | Body |
|---|---|---|
| GET | `/experiences?page=0&size=10&categoryId=&title=` | — |
| GET | `/experiences/{experienceId}` | — |
| POST | `/experiences` | **multipart/form-data** |
| PUT | `/experiences/{experienceId}` | **multipart/form-data** (image opcional) |
| DELETE | `/experiences/{experienceId}` | — |

### POST / PUT — multipart/form-data
Dos partes:

| parte | tipo | valor |
|---|---|---|
| `experience` | **Text con Content-Type `application/json`** | JSON de abajo |
| `image` | **File** | un `.jpg`/`.png` cualquiera (obligatorio en POST, opcional en PUT) |

JSON de la parte `experience`:
```json
{
  "title": "City tour Buenos Aires",
  "description": "Recorrido guiado por el centro histórico",
  "price": 25000.00,
  "location": "Buenos Aires",
  "categoryId": 1
}
```

> En Insomnia: en la fila `experience` del form, botón derecho / menú de la fila → *Set content-type* → `application/json`.
> Si no, Spring devuelve **415 Unsupported Media Type**.
> Validaciones: `title` no vacío, `price > 0`, `categoryId` existente. Solo el **publisher** o un **ADMIN** pueden editar/borrar. No se puede borrar una experiencia con sesiones.

---

## 4. Sesiones — `/experience-sessions`

| Método | Ruta | Body |
|---|---|---|
| GET | `/experience-sessions?page=0&size=10&experienceId=` | — |
| GET | `/experience-sessions/experience/{experienceId}?page=0&size=10` | — |
| GET | `/experience-sessions/{sessionId}` | — |
| POST | `/experience-sessions` | ver abajo |
| PUT | `/experience-sessions/{sessionId}` | ver abajo (campos opcionales) |
| DELETE | `/experience-sessions/{sessionId}` | — |

### POST
```json
{
  "experienceId": 1,
  "startsAt": "2026-10-15T10:00:00",
  "endsAt": "2026-10-15T13:00:00",
  "capacity": 20
}
```

### PUT (todo opcional)
```json
{
  "startsAt": "2026-10-16T09:00:00",
  "endsAt": "2026-10-16T12:00:00",
  "capacity": 25
}
```

> `startsAt` debe ser futuro, `endsAt` posterior a `startsAt`, `capacity > 0`, sin solapamiento con otra sesión de la misma experiencia. `availableSeats` se inicializa con `capacity`. No se puede borrar una sesión con reservas.

---

## 5. Reseñas — `/reviews`

| Método | Ruta | Body |
|---|---|---|
| GET | `/reviews?page=0&size=10` | — |
| GET | `/reviews/experience/{experienceId}?page=0&size=10` | — |
| GET | `/reviews/{reviewId}` | — |
| POST | `/reviews` | ver abajo |
| DELETE | `/reviews/{reviewId}` | — (autor o ADMIN) |

### POST
```json
{
  "experienceId": 1,
  "rating": 5,
  "comment": "Excelente experiencia, muy recomendable"
}
```

> `rating` entre 1 y 5. Un usuario solo puede dejar **una** reseña por experiencia (segunda → 400). El autor viene del token.

---

## 6. Carrito — `/carts`  ⚠️ STUB

Los métodos del `CartService` están sin implementar (parte de P3). Los endpoints existen pero **devuelven `null` / 200 vacío**. Se dejan en la colección para mostrar el contrato.

| Método | Ruta | Body |
|---|---|---|
| GET | `/carts/user/{userId}` | — |
| POST | `/carts/items` | `{ "userId": 1, "experienceSessionId": 1, "quantity": 2 }` |
| PATCH | `/carts/user/{userId}/items/{cartItemId}?quantity=3` | — |
| DELETE | `/carts/user/{userId}/items/{cartItemId}` | — |
| DELETE | `/carts/user/{userId}` | — |

---

## 7. Órdenes — `/orders`

| Método | Ruta | Body |
|---|---|---|
| GET | `/orders?page=0&size=10` | — (propias; ADMIN ve todas) |
| GET | `/orders/{orderId}` | — |
| POST | `/orders` | opcional, ver abajo |

### POST `/orders` (checkout)
Toma **el carrito del usuario autenticado**, crea la orden + una `Booking` por item y descuenta `availableSeats`. Body opcional:
```json
{
  "couponCode": "VERANO2026"
}
```
O sin body / `{}` para no aplicar cupón.

> Requiere que el carrito tenga items. Como el carrito es un stub, para demostrar el checkout hay que **cargar `cart` + `cart_items` a mano en MySQL**. El `couponCode` se normaliza a mayúsculas y se valida vigencia + `active`.

---

## 8. Reservas — `/bookings`

| Método | Ruta |
|---|---|
| GET | `/bookings?page=0&size=10` (propias; ADMIN ve todas) |
| GET | `/bookings/{bookingId}` |

Solo lectura. Las reservas se generan en el checkout (`POST /orders`).

---

## 9. Cupones — `/discount-coupons`

| Método | Ruta | Permiso | Body |
|---|---|---|---|
| GET | `/discount-coupons?page=0&size=10` | CLIENTE/ADMIN | — |
| GET | `/discount-coupons/{couponId}` | CLIENTE/ADMIN | — |
| POST | `/discount-coupons` | ADMIN | ver abajo |
| PUT | `/discount-coupons/{couponId}` | ADMIN | ver abajo (opcional) |
| DELETE | `/discount-coupons/{couponId}` | ADMIN | — |

### POST
```json
{
  "code": "VERANO2026",
  "percentage": 15,
  "validFrom": "2026-09-01T00:00:00",
  "validUntil": "2026-12-31T23:59:59",
  "active": true
}
```

### PUT (todo opcional)
```json
{
  "percentage": 20,
  "active": false
}
```

> `code` se guarda en mayúsculas y es único. `percentage` entre 1 y 100. `validFrom < validUntil`. Al borrar un cupón ya usado en una orden, no se elimina: se pone `active = false`.

---

## Flujo sugerido para la demo

1. `POST /api/v1/auth/register` → cliente.
2. `POST /api/v1/auth/authenticate` → copiar token al environment.
3. (como ADMIN) `POST /experience-categories`.
4. (como ADMIN) `POST /experiences` (multipart).
5. `POST /experience-sessions`.
6. `GET /experiences`, `GET /experience-sessions/experience/1`.
7. `POST /reviews`.
8. (ADMIN) `POST /discount-coupons`.
9. Cargar `cart` + `cart_items` en MySQL → `POST /orders` con `couponCode`.
10. `GET /orders`, `GET /bookings`.
