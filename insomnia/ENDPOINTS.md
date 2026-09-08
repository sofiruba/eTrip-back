# eTrip - Endpoints para Insomnia

**Base URL:** `http://localhost:4002` (sin context-path)
**Puerto:** 4002 · **DB:** MySQL `etrip_db`

## Glosario — nombres de clases en inglés

Todo el código (entidades, DTOs, endpoints) está en inglés. Por si en algún momento se
confunde el nombre en la carpeta/entidad con lo que se ve en pantalla:

| Inglés (código) | Español (lo que representa) |
|---|---|
| `Booking` | Reserva / voucher individual |
| `Order` | Orden — el comprobante que agrupa las `Booking` de un mismo checkout (no es "orden de compra" de cara al usuario, es interno) |
| `Cart` / `CartItem` | Carrito / item del carrito |
| `Experience` | Experiencia (el "producto") |
| `ExperienceCategory` | Categoría de experiencia |
| `ExperienceSession` | Sesión / turno |
| `ExperienceImage` | Foto de una experiencia |
| `DiscountCoupon` | Cupón de descuento |
| `Review` | Reseña |
| `User` | Usuario |

## Cómo usar

1. Insomnia → *Import* → `eTrip_insomnia.json` (ya trae carpetas + environment "Local").
2. **Levantar el backend primero** (`./mvnw spring-boot:run`, ver el README para la config de
   MySQL). Si el backend no está corriendo, Insomnia tira error de conexión, no 403.
3. Correr **POST register** (pide `username` además de nombre/apellido/mail/contraseña) o, si ya
   tenés usuario, **POST authenticate**. **No hace falta copiar nada a mano**: las 3 requests de
   `01 - Auth` tienen un *After-Response Script* que guarda `access_token` en la variable `token`
   del environment, y además saca el `id` del usuario (viaja como claim `id` adentro del JWT, ver
   `JwtService.buildToken`) y lo guarda en `user_id`. Cada vez que volvés a loguear (otro usuario,
   otro rol) las dos variables se pisan solas con el último login que hiciste.
4. Todas las requests menos `/api/v1/auth/**` mandan `Authorization: Bearer {{ token }}`; las de
   carrito (`/carts/user/{{ _.user_id }}/...`) usan `{{ _.user_id }}` para el usuario dueño.

> Si tu Insomnia es muy viejo y no corre los After-Response Scripts, hacelo a mano: copiar
> `access_token` de la respuesta → `Manage Environments` → variable `token`; y el `id` de la
> respuesta de `GET /users/me` → variable `user_id`.

**El 403 más común no es un bug: es que `token` está vacío o vencido** (dura 24hs). Si te tira 403
en todo, repetí el paso 3. Si te tira 403 puntual en un endpoint (ej. crear un cupón, listar
usuarios), es porque ese endpoint es solo ADMIN y tu usuario es CLIENTE — ver la tabla de abajo.

## Reglas de seguridad

| Recurso | Permisos |
|---|---|
| `/api/v1/auth/**` | público |
| `/users/**` | CLIENTE o ADMIN (algunas acciones dentro, como `GET /users` o `PATCH .../role`, exigen ADMIN y devuelven 403 si no) |
| `/experiences`, `/experience-categories`, `/experience-sessions` | CLIENTE o ADMIN |
| `/reviews`, `/carts`, `/orders`, `/bookings` | CLIENTE o ADMIN |
| `GET /discount-coupons/**` | CLIENTE o ADMIN |
| `POST/PUT/DELETE /discount-coupons/**` | solo ADMIN |

- **`register` siempre crea rol `CLIENTE`** (el campo `role` del body, si lo mandás, se ignora).
- Para tener un ADMIN: `UPDATE user SET role='ADMIN' WHERE email='...';` en MySQL y volver a hacer
  `authenticate` para obtener un token nuevo (el rol viaja adentro del JWT). También podés pedirle
  a otro ADMIN que te lo asigne con `PATCH /users/{id}/role?role=ADMIN`.
- Fechas: formato ISO local sin zona → `2026-10-15T10:00:00`.

---

## 1. Auth — `/api/v1/auth` (público)

### POST `/api/v1/auth/register`
```json
{
  "username": "nachoc",
  "firstname": "Nacho",
  "lastname": "Cortes",
  "email": "nacho@etrip.com",
  "password": "1234"
}
```
`username` y `email` son únicos. Si falta cualquier campo, o el username/email ya existe → `400`.

### POST `/api/v1/auth/authenticate`
Acepta **email o username** en el mismo campo `usernameOrEmail`:
```json
{
  "usernameOrEmail": "nacho@etrip.com",
  "password": "1234"
}
```
o
```json
{
  "usernameOrEmail": "nachoc",
  "password": "1234"
}
```
Respuesta: `{ "access_token": "eyJ..." }`

---

## 2. Usuarios y Perfil — `/users`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/users/me` | propio | Mi perfil: `username`, nombre, `role`, `email`, `publishedExperiences`, `bookingsCount`, `reviewsCount`. |
| PUT | `/users/me` | propio | Editar `firstName`/`lastName` (al menos uno). |
| GET | `/users/{userId}` | cualquiera logueado | Perfil público de otro usuario (sin email/contadores privados salvo que seas vos o ADMIN). |
| GET | `/users?page=&size=` | **ADMIN** | Lista de usuarios. |
| PATCH | `/users/{userId}/role?role=CLIENTE\|ADMIN` | **ADMIN** | Asigna permisos. No podés cambiarte tu propio rol (403). |

### PUT `/users/me`
```json
{ "firstName": "Nacho", "lastName": "Cortes" }
```

---

## 3. Categorías — `/experience-categories`

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

## 4. Experiencias — `/experiences`

| Método | Ruta | Body |
|---|---|---|
| GET | `/experiences?page=&size=&...filtros` | — (ver filtros abajo) |
| GET | `/experiences/mine?page=&size=` | — (mis publicaciones, modo vendedor) |
| GET | `/experiences/{experienceId}` | — |
| POST | `/experiences` | **multipart/form-data** |
| PUT | `/experiences/{experienceId}` | **multipart/form-data** (`images` opcional) |
| PATCH | `/experiences/{experienceId}/discount` | JSON, ver abajo |
| DELETE | `/experiences/{experienceId}` | — |

### Filtros de `GET /experiences` (todos opcionales, se combinan con AND)

| Param | Efecto |
|---|---|
| `categoryId` | categoría exacta (`404` si no existe) |
| `title` / `location` | coincidencia parcial, case-insensitive |
| `minPrice` / `maxPrice` | rango sobre el precio de lista (`minPrice>maxPrice` → `400`) |
| `publisherId` | experiencias de un vendedor puntual |
| `onlyDiscounted=true` | solo experiencias con descuento activo ("ofertas") |
| `dateFrom` / `dateTo` | experiencias con una sesión en ese rango de fechas |

Ejemplo: `/experiences?location=Palermo&minPrice=1000&maxPrice=50000&onlyDiscounted=true`

### POST / PUT — multipart/form-data
Dos tipos de parte:

| parte | tipo | valor |
|---|---|---|
| `experience` | **Text con Content-Type `application/json`** | JSON de abajo |
| `images` | **File**, repetida una vez por foto | una o más fotos (obligatorio al menos 1 en POST; en PUT, si no mandás ninguna, se mantienen las que ya tenía; si mandás una o más, **reemplazan** todo el set) |

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

> En Insomnia: en la fila `experience` del form, botón derecho / menú de la fila → *Set content-type* → `application/json`. Si no, Spring devuelve **415 Unsupported Media Type**.
> **La fila `images` (File) hay que llenarla a mano**: click en el ícono de archivo de esa fila y
> elegí una foto real de tu disco antes de mandar. **Si dejás una fila `images` sin archivo
> seleccionado, Insomnia no puede armar el request y no manda nada** (no es un 400 del backend,
> ni siquiera llega — por eso puede parecer que "no se puede subir nada"). La colección trae **una
> sola** fila `images` para no pisarse con esto; si querés más de 1 foto, duplicá la fila (mismo
> nombre `images`) con el botón `+` del form, y llenala también.
> Validaciones: `title` no vacío, `price > 0`, `categoryId` existente, al menos 1 foto no vacía,
> **`title` no repetido entre las experiencias del mismo publisher** (case-insensitive; el mismo
> título lo puede usar otro vendedor distinto, no es único globalmente) → `400`. Solo el
> **publisher** o un **ADMIN** pueden editar/borrar. No se puede borrar una experiencia con sesiones.

### PATCH `/experiences/{id}/discount` — descuento sobre el producto individual
```json
{ "discountPercentage": 20 }
```
Solo el dueño de la experiencia o un ADMIN. `discountPercentage` debe ser `0 <= x < 100` (`400` si no); `0` o no mandar el campo saca el descuento. El precio final (`finalPrice` en la respuesta, y el que se usa en el checkout) sale de `price * (1 - discountPercentage/100)`.

---

## 5. Sesiones — `/experience-sessions`

| Método | Ruta | Body |
|---|---|---|
| GET | `/experience-sessions?page=&size=&...filtros` | — (ver filtros abajo) |
| GET | `/experience-sessions/experience/{experienceId}?page=&size=&onlyAvailable=` | — |
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

### Filtros de `GET /experience-sessions` (opcionales, se combinan con AND)

| Param | Efecto |
|---|---|
| `experienceId` | sesiones de una experiencia puntual (`404` si no existe) |
| `onlyAvailable=true` | solo sesiones con `availableSeats > 0` |
| `dateFrom` / `dateTo` | sesiones con `startsAt` en ese rango |

Ejemplo: `/experience-sessions?experienceId=1&onlyAvailable=true&dateFrom=2027-01-01T00:00:00`

> `startsAt` debe ser futuro, `endsAt` posterior a `startsAt`, `capacity > 0`, sin solapamiento
> (ni exacto ni parcial) con otra sesión de la misma experiencia — un `POST` con las mismas
> fechas que una sesión ya creada da `400`, no duplica nada. `availableSeats` se inicializa con
> `capacity`. No se puede borrar una sesión con reservas. Si no hay `availableSeats`, no se puede
> agregar al carrito (lo valida el módulo de Carrito). **`POST`/`PUT`/`DELETE` son solo para el
> dueño (publisher) de la experiencia o un `ADMIN`** (`403` en otro caso) — el `GET` sigue
> abierto a cualquier `CLIENTE`/`ADMIN` logueado.

---

## 6. Reseñas — `/reviews`

| Método | Ruta | Body |
|---|---|---|
| GET | `/reviews?page=0&size=10` | — **CLIENTE: solo las propias. ADMIN: todas.** |
| GET | `/reviews/experience/{experienceId}?page=0&size=10` | — **pública**, cualquier logueado (son la opinión sobre un producto del catálogo, no datos privados) |
| GET | `/reviews/mine?page=0&size=10` | — mis reseñas (igual que `GET /reviews` para un CLIENTE, queda como alias explícito) |
| GET | `/reviews/{reviewId}` | — **CLIENTE: solo si es autor (`403` si no). ADMIN: cualquiera.** |
| POST | `/reviews` | ver abajo |
| DELETE | `/reviews/{reviewId}` | — (autor o ADMIN) |

> **Por qué `/reviews/experience/{id}` queda pública y las demás no:** esas son las reseñas *de
> un producto* — se muestran junto a la experiencia para que el que está por reservar las lea,
> igual que el `averageRating` que ya viaja en `ExperienceResponseDTO`. Restringirlas a "solo las
> mías" rompería el caso de uso de leer opiniones ajenas antes de comprar. `GET /reviews` (la
> lista global, sin filtrar por experiencia) y `GET /reviews/{id}` sí se restringieron: son la
> vista "todas las reseñas de todo el mundo", que no tiene sentido para un CLIENTE.

### POST
```json
{
  "experienceId": 1,
  "rating": 5,
  "comment": "Excelente experiencia, muy recomendable"
}
```

> `rating` entre 1 y 5. Un usuario solo puede dejar **una** reseña por experiencia (la segunda → `400`). El autor viene del token, no del body.

---

## 7. Carrito — `/carts`

Implementado (Facundo Etchart, `feature/carrito`). El `userId` de la ruta/body se valida contra
el usuario logueado (`403` si no coincide y no sos ADMIN).

| Método | Ruta | Body |
|---|---|---|
| GET | `/carts/user/{userId}` | — (crea el carrito vacío la primera vez) |
| POST | `/carts/items` | `{ "experienceSessionId": 1, "quantity": 2 }` — `userId` es **opcional**, si no lo mandás se usa el del token (si lo mandás, tiene que ser el tuyo o sos ADMIN). Si ya estaba, suma cantidad. |
| PATCH | `/carts/user/{userId}/items/{cartItemId}?quantity=3` | — |
| DELETE | `/carts/user/{userId}/items/{cartItemId}` | — |
| DELETE | `/carts/user/{userId}` | — (vacía el carrito) |

> Las 4 rutas que dicen `/user/{userId}` en el path sí necesitan el id ahí (no hay forma de
> omitirlo en la URL) — usá `{{ _.user_id }}`, que se llena solo con el último login (ver sección
> **Cómo usar**). Solo `POST /carts/items` tiene el `userId` en el body, y ese sí es opcional.

> Validaciones: `quantity > 0`; no se puede superar `availableSeats` de la sesión (`400` — cubre
> "si no hay stock no se puede agregar al carrito" de la consigna); tocar un item que no es tuyo
> → `403`. `unitPrice`/`subtotal` de cada item ya vienen con el descuento del producto aplicado
> (si tiene), así que coinciden con lo que después cobra `POST /orders`.

---

## 8. Órdenes — `/orders`

| Método | Ruta | Body |
|---|---|---|
| GET | `/orders?page=0&size=10` | — (propias; ADMIN ve todas) |
| GET | `/orders/{orderId}` | — |
| POST | `/orders` | opcional, ver abajo |

### POST `/orders` (checkout)
Toma **el carrito del usuario autenticado**, calcula el subtotal usando el **precio con descuento**
de cada experiencia (si tiene), crea la orden + una `Booking` con voucher por cada item, descuenta
`availableSeats` y vacía el carrito. Body opcional:
```json
{
  "couponCode": "VERANO2026"
}
```
O sin body / `{}` para no aplicar cupón.

> Requiere que el carrito tenga items. Mientras el carrito sea un stub, para probar el checkout hay
> que **cargar `carts` + `cart_items` a mano en MySQL**. El `couponCode` se normaliza a mayúsculas
> y se valida vigencia + `active` (mismo criterio que `GET /discount-coupons/validate`).

---

## 9. Reservas — `/bookings`

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/bookings?page=0&size=10` | Mis vouchers (ADMIN ve todos). |
| GET | `/bookings/sales?page=0&size=10` | **Modo vendedor:** reservas sobre las experiencias que publiqué, con `buyerId`/`buyerName`. |
| GET | `/bookings/experience/{experienceId}?page=0&size=10` | Reservas de una experiencia puntual. Solo el dueño de esa experiencia o ADMIN (403/404 si no). |
| GET | `/bookings/{bookingId}` | Una reserva. La ve el comprador, el vendedor de esa experiencia, o ADMIN. |

Solo lectura: las reservas se generan en el checkout (`POST /orders`).

---

## 10. Cupones — `/discount-coupons`

| Método | Ruta | Permiso | Body |
|---|---|---|---|
| GET | `/discount-coupons?page=0&size=10` | CLIENTE/ADMIN | — |
| GET | `/discount-coupons/validate?code=` | CLIENTE/ADMIN | — (chequeo antes de pagar, ver abajo) |
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

### GET `/discount-coupons/validate?code=VERANO2026`
Siempre devuelve `200`, nunca `404`:
```json
{ "code": "VERANO2026", "valid": true, "reason": null, "percentage": 15.00 }
```
`reason` cuando `valid=false`: `NOT_FOUND`, `INACTIVE`, `NOT_YET_VALID`, `EXPIRED`,
`INVALID_PERCENTAGE`. Pensado para que el front lo llame apenas el usuario tipea el código, antes
de tocar `POST /orders`.

> `code` se guarda en mayúsculas y es único (case-insensitive). `percentage` entre 1 y 100.
> `validFrom < validUntil`. Al borrar un cupón ya usado en una orden, no se elimina: se pone
> `active = false`.

---

## Flujo sugerido para la demo

1. `POST /api/v1/auth/register` (con `username`) → vendedor.
2. `POST /api/v1/auth/authenticate` → copiar token al environment.
3. `POST /experience-categories`.
4. `POST /experiences` (multipart, dos o más `images`).
5. `PATCH /experiences/{id}/discount` → mostrar `finalPrice`.
6. `POST /experience-sessions`.
7. Registrar un segundo usuario (comprador), `GET /experiences?onlyDiscounted=true`,
   `GET /experiences/mine` (con el vendedor).
8. `POST /reviews` (comprador) y mostrar que la segunda reseña a la misma experiencia da `400`.
9. `PUT /users/me`, `GET /users/{id}` (perfil público del comprador visto por el vendedor).
10. (ADMIN) `POST /discount-coupons`, `GET /discount-coupons/validate?code=`.
11. `GET /carts/user/{id}` (se crea solo), `POST /carts/items`, `PATCH .../items/{id}?quantity=`
    → `POST /orders` con `couponCode` → mostrar que el `subtotal` ya usa el precio con descuento.
12. `GET /orders`, `GET /bookings`, `GET /bookings/sales` (vendedor viendo quién le compró).
13. (ADMIN) `GET /users`, `PATCH /users/{id}/role`.
