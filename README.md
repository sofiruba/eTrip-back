# eTrip — Backend

Marketplace de experiencias turísticas locales (clases de cocina, tours, caminatas urbanas,
eventos gastronómicos), estilo Airbnb Experiences. TPO grupal de Aplicaciones Interactivas
(UADE, 2º cuatrimestre 2026).

La consigna original de la cátedra es un e-commerce genérico; se adaptó al dominio de
experiencias turísticas.

## Flujo

```
Usuario → Carrito → Confirmación de reserva → Bookings / Vouchers
```

Un usuario publica una **experiencia** dentro de una **categoría**. Cada experiencia tiene
**sesiones/turnos** (fecha, capacidad, cupos). Otro usuario arma un **carrito** con sesiones,
opcionalmente aplica un **cupón de descuento**, y al confirmar se generan las **reservas
(bookings)** con su voucher, descontando los cupos.

`Order` existe como entidad interna que agrupa los vouchers de una compra; de cara al usuario
no hay un paso separado de "orden".

## Stack

- Java 17 + Spring Boot 3.1.11
- Spring Data JPA / Hibernate
- MySQL 8
- Spring Security + JWT
- Lombok
- Maven (wrapper incluido: `./mvnw`)

## Arquitectura

- `Controller → Service → Repository`. Sin lógica de negocio en los controllers.
- Validación manual en los `*ServiceImpl` (sin `spring-boot-starter-validation`).
- Los endpoints exponen DTOs de request/response, nunca entidades.
- Inyección de dependencias por constructor (`@RequiredArgsConstructor`).
- Excepciones checked con `@ResponseStatus`: `ResourceNotFoundException` (404),
  `BadRequestException` (400), `ForbiddenException` (403), `CategoryDuplicateException` (400).
- Roles: enum `Role` (`CLIENTE`, `ADMIN`). Autorización por ruta en `SecurityConfig`
  (`hasAnyAuthority`); reglas de dueño / auto-asignación en el service.
- Paginación con `Page`/`PageRequest`; sin `page`/`size` devuelve todo en una página.

## Modelo de dominio

| Entidad | Tabla | Campos clave | Relaciones |
|---|---|---|---|
| `User` | `user` | `email` (único), `displayUsername` (columna `username`, único), `password`, `firstName`, `lastName`, `role` | 1—N `Experience` (publisher), 1—N `Order`, 1—1 `Cart` |
| `ExperienceCategory` | `experience_categories` | `name` (único), `description` | 1—N `Experience` |
| `Experience` | `experiences` | `title`, `description`, `price`, `discountPercentage` (nullable), `location` | N—1 `ExperienceCategory`, N—1 `User` (publisher), 1—N `ExperienceImage`, 1—N `ExperienceSession`, 1—N `Review` |
| `ExperienceImage` | `experience_images` | `image` (`byte[]`/`LONGBLOB`), `position` | N—1 `Experience` |
| `ExperienceSession` | `experience_sessions` | `startsAt`, `endsAt`, `capacity`, `availableSeats` | N—1 `Experience`, 1—N `Booking` |
| `Cart` | `carts` | — | 1—1 `User`, 1—N `CartItem` |
| `CartItem` | `cart_items` | `quantity` | N—1 `Cart`, N—1 `ExperienceSession` |
| `DiscountCoupon` | `discount_coupons` | `code` (único), `percentage`, `validFrom`, `validUntil`, `active` | 1—N `Order` |
| `Order` | `orders` | `createdAt`, `subtotal`, `discountAmount`, `total` | N—1 `User`, N—1 `DiscountCoupon` (opcional), 1—N `Booking` |
| `Booking` | `bookings` | `voucherCode` (`ETRIP-XXXXXXXX`), `quantity`, `createdAt` | N—1 `Order`, N—1 `ExperienceSession` |
| `Review` | `reviews` | `rating` (1-5), `comment`, `createdAt` | N—1 `User`, N—1 `Experience` |

Notas:
- Precio final: `Experience.getEffectivePrice()` (`price * (1 - discountPercentage/100)`). Lo usan
  `ExperienceResponseDTO.finalPrice`, el carrito y el checkout.
- `User.displayUsername` mapea a la columna `username`; el nombre del campo evita colisión con
  `UserDetails.getUsername()` (que Spring Security usa y debe devolver el email).
- `ExperienceImage` es una entidad aparte (`cascade = ALL, orphanRemoval = true`) porque una
  experiencia puede tener una o más fotos.

## Cómo correr

1. MySQL 8 corriendo en `localhost:3306`.
2. Crear la base:
   ```sql
   CREATE DATABASE etrip_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. Ajustar usuario/contraseña en
   [`src/main/resources/application.properties`](src/main/resources/application.properties), o
   sobreescribir con variables de entorno `DB_USER` / `DB_PASSWORD`.
4. Levantar:
   ```bash
   ./mvnw spring-boot:run
   ```
5. API en `http://localhost:4002`. Hibernate crea/actualiza las tablas al arrancar
   (`spring.jpa.hibernate.ddl-auto=update`).

### Autenticación

```bash
# Registro
curl -X POST http://localhost:4002/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"anap","firstname":"Ana","lastname":"Perez","email":"ana@test.com","password":"pass1234"}'

# Login (usernameOrEmail acepta username o email)
curl -X POST http://localhost:4002/api/v1/auth/authenticate \
  -H 'Content-Type: application/json' \
  -d '{"usernameOrEmail":"ana@test.com","password":"pass1234"}'
```

Ambos devuelven `{ "access_token": "<JWT>" }`. Ese token va en el header
`Authorization: Bearer <JWT>` en el resto de los endpoints. `register` siempre crea rol
`CLIENTE`. Para tener un `ADMIN`: `UPDATE user SET role='ADMIN' WHERE email=...` en MySQL, o
que otro ADMIN use `PATCH /users/{id}/role`.

## JWT y autenticación

| Archivo | Rol |
|---|---|
| `JwtService` | Genera y valida el JWT (HMAC-SHA, expira a las 24hs). |
| `AuthenticationService` | `register`/`authenticate`: valida datos, encripta password (BCrypt), pide el token. |
| `ApplicationConfig` | `UserDetailsService`, `AuthenticationProvider`, `PasswordEncoder`. |
| `JwtAuthenticationFilter` | Corre en cada request: valida el token y carga el usuario en el `SecurityContext`. |
| `SecurityConfig` | Rutas públicas y rol requerido por ruta. |

Payload del JWT: `sub` (email), `id` (id numérico del usuario), `iat`/`exp`. El rol no viaja en
el token — se relee de la base en cada request, así un cambio de rol aplica sin renovar token.

El proyecto no tiene `AuthenticationEntryPoint` custom: token faltante, inválido, vencido, o rol
insuficiente devuelven todos **403** (no hay 401 separado).

## Módulos implementados

| Módulo | Recursos | Estado |
|---|---|---|
| Catálogo | categorías, experiencias, fotos, descuento por producto | ✅ |
| Sesiones / Turnos | cupos y horarios por experiencia | ✅ |
| Carrito | armado del carrito, validación de stock | ✅ |
| Reservas y cupones | checkout, vouchers, cupones de descuento globales | ✅ |
| Usuarios y perfil | perfil propio/ajeno, administración de roles | ✅ |
| Reseñas | rating + comentario por experiencia | ✅ |

## Flujos principales

**Anfitrión:**
```
register/authenticate → POST /experience-categories (si hace falta)
  → POST /experiences (multipart, 1+ fotos) → PATCH /experiences/{id}/discount (opcional)
  → POST /experience-sessions → GET /experiences/mine → GET /bookings/sales
```

**Comprador:**
```
register/authenticate → GET /experiences?...filtros → GET /experiences/{id}
  → POST /carts/items → GET /discount-coupons/validate?code= (opcional)
  → POST /orders (checkout) → GET /orders, GET /bookings → POST /reviews
```

**Administración:**
```
authenticate (usuario ADMIN) → GET /users → PATCH /users/{id}/role
  → POST/PUT/DELETE /discount-coupons
```

---

## Endpoints — Categorías de experiencias

Base: `/experience-categories` · Rol `CLIENTE` o `ADMIN`.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/experience-categories?page=&size=` | Lista paginada. Sin `page`/`size` devuelve todo. |
| `GET` | `/experience-categories/{id}` | Categoría por id. `404` si no existe. |
| `POST` | `/experience-categories` | Crea. Body: `{ "name", "description" }`. |
| `PUT` | `/experience-categories/{id}` | Actualiza `name` / `description`. |
| `DELETE` | `/experience-categories/{id}` | Elimina. `400` si tiene experiencias asociadas. |

**Validaciones:** `name` obligatorio (`400`); `name` único → `400`.

## Endpoints — Experiencias

Base: `/experiences` · Rol `CLIENTE` o `ADMIN`.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/experiences?page=&size=&...` | Lista paginada con filtros (ver abajo). |
| `GET` | `/experiences/mine?page=&size=` | Mis experiencias publicadas. |
| `GET` | `/experiences/{id}` | Experiencia por id. `404` si no existe. |
| `POST` | `/experiences` | `multipart/form-data`: parte `experience` (JSON) + una o más partes `images` (mínimo 1). |
| `PUT` | `/experiences/{id}` | Actualiza. `images` opcional (si viene, reemplaza el set). Dueño o `ADMIN`. |
| `PATCH` | `/experiences/{id}/discount` | Body: `{ "discountPercentage": 20 }`. `0 <= x < 100`. Dueño o `ADMIN`. |
| `DELETE` | `/experiences/{id}` | Dueño o `ADMIN`. `400` si tiene sesiones asociadas. |

**Filtros de `GET /experiences`** (opcionales, se combinan con AND):

| Param | Efecto |
|---|---|
| `categoryId` | Categoría exacta. `404` si no existe. |
| `title` | Coincidencia parcial en el título. |
| `location` | Coincidencia parcial en la ubicación. |
| `minPrice` / `maxPrice` | Rango de precio. `minPrice > maxPrice` → `400`. |
| `publisherId` | Experiencias de un anfitrión puntual. |
| `onlyDiscounted` | Solo con descuento de producto activo. |
| `dateFrom` / `dateTo` | Al menos una sesión con `startsAt` en rango. |

**Body de `experience`:**
```json
{ "title": "Pastas caseras", "description": "Amasado y salsa", "price": 5000, "location": "Palermo", "categoryId": 1 }
```

**Reglas:**
- `title`, `price` > 0, `categoryId` obligatorios; al menos 1 foto → si no, `400`.
- `title` único por publisher (no global) → `400`.
- Categoría debe existir → `404`.
- Publisher se toma del JWT, no del body.
- Fotos se guardan como `byte[]` (comprimidas a JPEG antes de guardar) y viajan como Base64 en
  `imagesBase64`.

## Endpoints — Sesiones / Turnos

Base: `/experience-sessions` · Rol `CLIENTE` o `ADMIN`.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/experience-sessions?page=&size=&experienceId=&onlyAvailable=&dateFrom=&dateTo=` | Lista con filtros combinables. |
| `GET` | `/experience-sessions/experience/{experienceId}?onlyAvailable=` | Sesiones de una experiencia. |
| `GET` | `/experience-sessions/{sessionId}` | Sesión por id. `404` si no existe. |
| `POST` | `/experience-sessions` | Body: `{ "experienceId", "startsAt", "endsAt", "capacity" }`. |
| `PUT` | `/experience-sessions/{sessionId}` | Actualiza (campos opcionales). |
| `DELETE` | `/experience-sessions/{sessionId}` | `400` si tiene reservas o está en algún carrito. |

**Reglas:** `startsAt` < `endsAt`, `startsAt` a futuro, `capacity` > 0, sin solapamiento de
horario en la misma experiencia, no se puede bajar `capacity` debajo de lo ya reservado.
`POST`/`PUT`/`DELETE`: solo el publisher de la experiencia o un `ADMIN`.

## Endpoints — Carrito

Base: `/carts` · Rol `CLIENTE` o `ADMIN`. `userId` se valida contra el usuario autenticado.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/carts/user/{userId}` | Trae el carrito; lo crea vacío la primera vez. |
| `POST` | `/carts/items` | Body: `{ "userId"?, "experienceSessionId", "quantity" }`. Si `userId` no viene, se toma del token. |
| `PATCH` | `/carts/user/{userId}/items/{cartItemId}?quantity=` | Fija la cantidad de un item. |
| `DELETE` | `/carts/user/{userId}/items/{cartItemId}` | Elimina un item. |
| `DELETE` | `/carts/user/{userId}` | Vacía el carrito. |

**Reglas:** `quantity` > 0; no puede superar `availableSeats`; item ajeno → `403`. `unitPrice` /
`subtotal` usan `Experience.getEffectivePrice()`.

## Endpoints — Cupones de descuento

Base: `/discount-coupons`.

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `GET` | `/discount-coupons?page=&size=` | CLIENTE / ADMIN | Lista cupones. |
| `GET` | `/discount-coupons/validate?code=` | CLIENTE / ADMIN | Siempre `200`: `{ code, valid, reason, percentage }`. |
| `GET` | `/discount-coupons/{id}` | CLIENTE / ADMIN | Cupón por id. `404` si no existe. |
| `POST` | `/discount-coupons` | ADMIN | Body: `{ "code", "percentage", "validFrom?", "validUntil?", "active?" }`. |
| `PUT` | `/discount-coupons/{id}` | ADMIN | Actualiza (campos opcionales). |
| `DELETE` | `/discount-coupons/{id}` | ADMIN | Sin uso: borra. Ya usado: desactiva (`active=false`). |

**Reglas:** `code` único (case-insensitive); `0 < percentage <= 100`; `validFrom` < `validUntil`
si vienen ambas; un cupón se puede usar una sola vez por usuario.

## Endpoints — Reservas (`/orders` y `/bookings`)

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/orders` | Confirma el carrito. Body opcional: `{ "couponCode" }`. |
| `GET` | `/orders?page=&size=` | Mis órdenes (ADMIN ve todas). |
| `GET` | `/orders/{orderId}` | Orden por id. `403` si no es propia ni ADMIN. |
| `GET` | `/bookings?page=&size=` | Mis vouchers (ADMIN ve todos). |
| `GET` | `/bookings/sales?page=&size=` | Reservas sobre mis experiencias publicadas. |
| `GET` | `/bookings/experience/{experienceId}?page=&size=` | Reservas de una experiencia. Dueño o ADMIN. |
| `GET` | `/bookings/{bookingId}` | Voucher por id. Comprador, dueño de la experiencia, o ADMIN. |

**`POST /orders`:**
1. Busca el carrito (`404` si no existe), valida que no esté vacío (`400`).
2. Revalida `availableSeats` de cada sesión (`400` si no alcanza).
3. Calcula `subtotal` con `Experience.getEffectivePrice()`.
4. Si hay `couponCode`: valida vigencia y que no lo haya usado antes este usuario (`400`/`404`),
   calcula `discountAmount`.
5. `total = subtotal - discountAmount`.
6. Crea `Order` + un `Booking` por item (`voucherCode` `ETRIP-XXXXXXXX`).
7. Descuenta `availableSeats`, vacía el carrito.

## Endpoints — Usuarios y perfil

Base: `/users` · Requiere estar autenticado.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/users/me` | Mi perfil + contadores. |
| `PUT` | `/users/me` | Edita `firstName`/`lastName`. |
| `GET` | `/users/{id}` | Perfil de otro. Datos completos solo si sos vos o ADMIN. |
| `GET` | `/users?page=&size=` | ADMIN: lista de usuarios. |
| `PATCH` | `/users/{id}/role?role=CLIENTE\|ADMIN` | ADMIN: asigna rol. `403` si es tu propio rol. |

## Endpoints — Reseñas

Base: `/reviews` · Rol `CLIENTE` o `ADMIN`.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/reviews?page=&size=` | CLIENTE: solo propias. ADMIN: todas. |
| `GET` | `/reviews/experience/{experienceId}?page=&size=` | Reseñas de una experiencia (pública). |
| `GET` | `/reviews/mine?page=&size=` | Mis reseñas. |
| `GET` | `/reviews/{reviewId}` | Reseña por id. CLIENTE: solo si es autor. ADMIN: cualquiera. |
| `POST` | `/reviews` | Body: `{ "experienceId", "rating", "comment" }`. |
| `DELETE` | `/reviews/{reviewId}` | Autor o ADMIN. |

**Reglas:** `rating` 1-5; una reseña por usuario y experiencia; requiere haber reservado esa
experiencia antes; autor sale del token.

---

## Reglas de negocio destacadas

- Filtros combinables en `GET /experiences` (hasta 7 a la vez, con AND).
- `/orders` y `/bookings` filtran por JWT, no por parámetro de URL.
- `/discount-coupons/validate` no gasta el cupón ni toca el carrito.
- Sesión sin cupos → no se puede reservar/agregar al carrito.
- Cupón vencido/inactivo/ya usado por ese usuario → `400`.
- Categoría/experiencia/sesión con relaciones no se puede borrar → `400`.
- Username y email únicos al registrarse.
- Rol de ADMIN no se puede auto-asignar/auto-revocar.

## Códigos de error

| Código | Cuándo |
|---|---|
| `400` | Datos inválidos o duplicado (`BadRequestException`, `CategoryDuplicateException`). |
| `403` | Recurso ajeno sin ser ADMIN, o token faltante/inválido/rol insuficiente. |
| `404` | Recurso inexistente (`ResourceNotFoundException`). |

## Qué falta

- Sin tests automatizados.
- No hay chequeo `publisher != buyer` en carrito/checkout.
- Sin recuperación/cambio de contraseña ni baja de cuenta.
- Sin CORS configurado.
- Sin Swagger/OpenAPI.

**Decisiones de diseño:**
- Un solo rol de comprador/vendedor (`CLIENTE`), no roles separados.
- Descuentos en dos niveles: por producto y por cupón global.
- Sin pantalla de "orden de compra" separada del checkout.

## Estructura del código

```
controllers/   REST controllers por recurso (auth, users, experiences, carts, orders, bookings, coupons, reviews, config)
service/       Interfaces de servicio + paquete impl/ con la lógica de negocio
repository/    Spring Data JPA (incluye Specifications para filtros dinámicos)
entity/        Entidades JPA
dtos/          request/ y response/, separados de las entidades
exceptions/    BadRequestException, ForbiddenException, ResourceNotFoundException, CategoryDuplicateException
```

## Puesta en marcha del proyecto base

Cambios sobre el proyecto base de la cátedra antes de implementar los módulos propios:

- Enum `Role`: `USER` → `CLIENTE`.
- Fix en `CategoryServiceImpl.createCategory()` (lanzaba excepción siempre).
- `User.lastName` tenía `unique = true` por error; se movió a `email`.
- Imagen de experiencia: `tinyblob` → `LONGBLOB`.
- `AuthenticationService.register` fuerza `Role.CLIENTE` (antes tomaba `role` del body).
