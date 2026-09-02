# eTrip — Backend

Marketplace de **experiencias turísticas locales** (clases de cocina, tours de estadios, caminatas
urbanas, eventos gastronómicos) al estilo *Airbnb Experiences*. TPO grupal de Aplicaciones
Interactivas (UADE, 2º cuatrimestre 2026).

La consigna original de la cátedra es un e-commerce genérico; el equipo la adaptó al dominio de
experiencias turísticas.

## Flujo de la aplicación

```
Usuario  →  Carrito  →  Confirmación de reserva  →  Bookings / Vouchers
```

Un **host** (usuario) publica una **experiencia** dentro de una **categoría**. Cada experiencia
tiene **sesiones/turnos** (fecha + capacidad + cupos). Otro usuario arma un **carrito** con sesiones,
opcionalmente aplica un **cupón de descuento**, y al confirmar se generan las **reservas (bookings)**
con su código de voucher, descontando los cupos de cada sesión.

No se usa el concepto de "orden de compra": el carrito se confirma directamente en reservas.

## Stack

- Java 17 + Spring Boot 3.1.11
- Spring Data JPA / Hibernate
- MySQL 8
- Spring Security + JWT (`JwtAuthenticationFilter` + `SecurityFilterChain`)
- Lombok
- Maven (wrapper incluido: `./mvnw`)

## Arquitectura

- **Controller → Service → Repository.** Sin lógica de negocio en los controllers.
- Toda la validación vive en los `*ServiceImpl`.
- Los endpoints exponen **DTOs** de request/response, nunca las entidades.
- Inyección de dependencias por constructor (`@RequiredArgsConstructor`).
- Manejo de errores con excepciones *checked* anotadas con `@ResponseStatus`
  (`ResourceNotFoundException` → 404, `BadRequestException` → 400, `ForbiddenException` → 403,
  `CategoryDuplicateException` → 400).
- Roles: enum `Role` (`CLIENTE`, `ADMIN`) como campo de `User`. La autorización por ruta se hace
  en `SecurityConfig` con `hasAnyAuthority(Role.X.name())`.

## Cómo correr

1. Tener MySQL 8 corriendo en `localhost:3306`.
2. Crear la base (si no existe):
   ```sql
   CREATE DATABASE etrip_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. Ajustar usuario/contraseña en [`src/main/resources/application.properties`](src/main/resources/application.properties)
   si hace falta (por defecto `root` / `hola1234`). También se puede sobreescribir sin tocar el
   archivo con variables de entorno `DB_USER` y `DB_PASSWORD`.
4. Levantar:
   ```bash
   ./mvnw spring-boot:run
   ```
5. La API queda en `http://localhost:4002`. Hibernate crea/actualiza las tablas al arrancar
   (`spring.jpa.hibernate.ddl-auto=update`).

### Autenticación (ya resuelta en el proyecto base)

```bash
# Registro
curl -X POST http://localhost:4002/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"firstname":"Ana","lastname":"Perez","email":"ana@test.com","password":"pass1234","role":"CLIENTE"}'

# Login
curl -X POST http://localhost:4002/api/v1/auth/authenticate \
  -H 'Content-Type: application/json' \
  -d '{"email":"ana@test.com","password":"pass1234"}'
```

Ambos devuelven `{ "access_token": "<JWT>" }`. Ese token va en el header
`Authorization: Bearer <JWT>` en todos los endpoints de abajo.

## División de módulos

| Persona | Módulo | Estado |
|---|---|---|
| 1 | **Catálogo** (categorías, experiencias, imágenes) | ✅ Implementado |
| 2 | **Sesiones / Turnos** | ✅ Implementado |
| 3 | Carrito | 🚧 En progreso (otro integrante) |
| 4 | **Reservas y Descuentos** (cupones, confirmación, vouchers) | ✅ Implementado |

Login y registro ya venían resueltos en el proyecto base. El registro público siempre crea
`CLIENTE`; los `ADMIN` se dan de alta a mano en la base.

> El proyecto base traía un ejemplo genérico de e-commerce (`Product` / `Category` /
> `CategoriesController`). Se eliminó porque el dominio real de eTrip usa
> `Experience` / `ExperienceCategory`.

---

# Módulo Catálogo (Persona 1) — Nacho Cortes

Responsable de **categorías de experiencias**, **experiencias** e **imágenes de experiencias**.

## Entidades

- **`ExperienceCategory`** (`experience_categories`): `id`, `name` (único, obligatorio), `description`.
- **`Experience`** (`experiences`): `id`, `title`, `description`, `price`, `location`,
  `image` (`byte[]`, `LONGBLOB`), FK a `ExperienceCategory` y a `User` (publisher).
- La imagen **no es una entidad aparte**: es una columna `byte[]` dentro de `Experience`. Se manda
  junto con la creación de la experiencia (multipart) y se devuelve como `imageBase64` en el
  response.

## Endpoints — Categorías de experiencias

Base: `/experience-categories` · Requiere rol `CLIENTE` o `ADMIN`.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/experience-categories?page=&size=` | Lista paginada. Sin `page`/`size` devuelve todo (igual que el ejemplo de la cátedra). |
| `GET` | `/experience-categories/{id}` | Categoría por id. `404` si no existe. |
| `POST` | `/experience-categories` | Crea. Body: `{ "name", "description" }`. |
| `PUT` | `/experience-categories/{id}` | Actualiza `name` / `description`. |
| `DELETE` | `/experience-categories/{id}` | Elimina. `400` si tiene experiencias asociadas. |

**Validaciones:** `name` obligatorio y no vacío (`400`); `name` único → `400`
(`CategoryDuplicateException`).

## Endpoints — Experiencias

Base: `/experiences` · Requiere rol `CLIENTE` o `ADMIN`.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/experiences?page=&size=&...` | Lista paginada con filtros (ver abajo). |
| `GET` | `/experiences/mine?page=&size=` | Mis experiencias publicadas (modo vendedor). |
| `GET` | `/experiences/{id}` | Experiencia por id. `404` si no existe. |
| `POST` | `/experiences` | Crea. `multipart/form-data`: parte `experience` (JSON) + parte `image` (archivo, **obligatoria**). |
| `PUT` | `/experiences/{id}` | Actualiza. `multipart/form-data`: `experience` (JSON) + `image` (opcional; si no viene, mantiene la actual). Solo el **dueño** o un **ADMIN**. |
| `DELETE` | `/experiences/{id}` | Elimina. Solo el **dueño** o un **ADMIN**. `400` si tiene sesiones asociadas. |

**Filtros de `GET /experiences`** (todos opcionales, se combinan con AND):

| Param | Efecto |
|---|---|
| `categoryId` | Categoría exacta. `404` si la categoría no existe. |
| `title` | Coincidencia parcial (case-insensitive) en el título. |
| `location` | Coincidencia parcial (case-insensitive) en la ubicación. |
| `minPrice` / `maxPrice` | Rango de precio. `minPrice > maxPrice` → `400`. |
| `publisherId` | Experiencias de un vendedor. |
| `dateFrom` / `dateTo` | Experiencias que tienen **al menos una sesión** con `startsAt` en ese rango (formato ISO `2027-06-15T10:00:00`). `dateFrom > dateTo` → `400`. |

Implementado con `JpaSpecificationExecutor` (`ExperienceSpecifications`).

**Body de `experience` (parte JSON):**
```json
{ "title": "Pastas caseras", "description": "Amasado y salsa", "price": 5000, "location": "Palermo", "categoryId": 1 }
```

**Reglas de negocio:**
- `title` obligatorio, `price` > 0, `categoryId` obligatorio → si no, `400`.
- La categoría debe existir → si no, `404`.
- El **publisher se toma del usuario autenticado** (JWT), no de un id en el body.
- `update` / `delete`: solo el usuario que publicó la experiencia o un `ADMIN` (`403` en otro caso).
- La imagen se guarda como `byte[]` en la base y se devuelve como Base64 en `ExperienceResponseDTO.imageBase64`.

**Ejemplo de creación:**
```bash
curl -X POST http://localhost:4002/experiences \
  -H "Authorization: Bearer $TOKEN" \
  -F 'experience={"title":"Pastas caseras","price":5000,"location":"Palermo","categoryId":1};type=application/json' \
  -F 'image=@foto.png;type=image/png'
```

## Códigos de error

| Código | Cuándo |
|---|---|
| `400` | Datos inválidos (`BadRequestException`) o categoría duplicada (`CategoryDuplicateException`). |
| `403` | Editar/eliminar una experiencia de otro usuario sin ser `ADMIN` (`ForbiddenException`), o request sin token. |
| `404` | Categoría / experiencia / usuario inexistente (`ResourceNotFoundException`). |

## Archivos del módulo

| Archivo | |
|---|---|
| `service/impl/ExperienceCategoryServiceImpl.java` | Lógica de categorías |
| `service/impl/ExperienceServiceImpl.java` | Lógica de experiencias + imagen |
| `controllers/experiences/ExperienceCategoriesController.java` | Endpoints de categorías |
| `controllers/experiences/ExperiencesController.java` | Endpoints de experiencias (multipart) |
| `service/ExperienceCategoryService.java`, `service/ExperienceService.java` | Interfaces |
| `dtos/request/ExperienceCategoryRequestDTO.java`, `dtos/response/ExperienceCategoryResponseDTO.java` | DTOs de categoría |
| `dtos/request/ExperienceRequestDTO.java`, `dtos/response/ExperienceResponseDTO.java` | DTOs de experiencia |
| `exceptions/BadRequestException.java`, `exceptions/ForbiddenException.java` | Excepciones nuevas (mismo estilo que las del base) |

## Puesta en marcha del proyecto base (previo al módulo)

Antes de implementar el catálogo hubo que dejar el proyecto compilando y arrancando:

- Migración del enum `Role`: `USER` → `CLIENTE` (entidad + `SecurityConfig`).
- Bug en `CategoryServiceImpl.createCategory()`: siempre lanzaba `CategoryDuplicateException` y no
  devolvía la categoría creada.
- `User.lastName` tenía `unique = true` (impedía dos usuarios con el mismo apellido); se movió el
  `unique` a `email`.
- `application.properties`: base `etrip_db` y credenciales.
- `Experience.image` se creaba como `tinyblob` (255 bytes); se forzó a `LONGBLOB`.

---

# Módulo Sesiones / Turnos (Persona 2)

Base: `/experience-sessions` · Requiere rol `CLIENTE` o `ADMIN`.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/experience-sessions?page=&size=&experienceId=` | Lista paginada (opcional filtrar por experiencia). |
| `GET` | `/experience-sessions/experience/{experienceId}` | Sesiones de una experiencia. |
| `GET` | `/experience-sessions/{sessionId}` | Sesión por id. `404` si no existe. |
| `POST` | `/experience-sessions` | Crea. Body: `{ "experienceId", "startsAt", "endsAt", "capacity" }`. |
| `PUT` | `/experience-sessions/{sessionId}` | Actualiza (campos opcionales). |
| `DELETE` | `/experience-sessions/{sessionId}` | Elimina. `400` si tiene reservas. |

**Reglas:** `startsAt` < `endsAt`, `startsAt` a futuro, `capacity` > 0, sin solapamiento de
horario dentro de la misma experiencia. Al crear, `availableSeats = capacity`.

---

# Módulo Carrito (Persona 3)

🚧 Lo está implementando otro integrante. Stubs en `CartServiceImpl` / `CartsController`.
El checkout de Persona 4 lee el carrito por la relación `Cart.items` (no depende de esos stubs).

---

# Módulo Reservas y Descuentos (Persona 4)

No se usa "orden de compra" como concepto de UX: el flujo es
**Carrito → confirmar reserva (`POST /orders`) → Bookings/Vouchers**. Internamente `Order` es el
comprobante que agrupa los vouchers y guarda subtotal / descuento / total / cupón.

## Cupones — `/discount-coupons`

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `GET` | `/discount-coupons?page=&size=` | CLIENTE / ADMIN | Lista cupones. |
| `GET` | `/discount-coupons/validate?code=` | CLIENTE / ADMIN | Chequea si un código sirve hoy. Siempre `200`: `{ code, valid, reason, percentage }` (`reason`: `NOT_FOUND` / `INACTIVE` / `NOT_YET_VALID` / `EXPIRED`). |
| `GET` | `/discount-coupons/{id}` | CLIENTE / ADMIN | Cupón por id. `404` si no existe. |
| `POST` | `/discount-coupons` | **ADMIN** | Crea. Body: `{ "code", "percentage", "validFrom?", "validUntil?", "active?" }`. |
| `PUT` | `/discount-coupons/{id}` | **ADMIN** | Actualiza (campos opcionales). |
| `DELETE` | `/discount-coupons/{id}` | **ADMIN** | Si el cupón nunca se usó lo borra; si ya se usó en una reserva lo **desactiva** (`active=false`). |

**Reglas:** `code` obligatorio y único (case-insensitive, se guarda en mayúsculas);
`0 < percentage <= 100`; si vienen ambas fechas, `validFrom` < `validUntil`. `active` por
defecto `true`.

## Reservas — `/orders` y `/bookings`

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/orders` | Confirma el carrito del usuario. Body opcional: `{ "couponCode" }`. |
| `GET` | `/orders?page=&size=` | Reservas del usuario autenticado (ADMIN ve todas). |
| `GET` | `/orders/{orderId}` | Reserva por id. `403` si no es del usuario ni ADMIN. |
| `GET` | `/bookings?page=&size=` | Mis vouchers (ADMIN ve todos). |
| `GET` | `/bookings/sales?page=&size=` | **Modo vendedor:** reservas sobre las experiencias que publiqué (trae `buyerId` / `buyerName`). |
| `GET` | `/bookings/experience/{experienceId}?page=&size=` | Reservas de una experiencia. Solo el dueño de la experiencia o un ADMIN (`403` / `404`). |
| `GET` | `/bookings/{bookingId}` | Voucher por id. Lo ve el comprador, el vendedor de esa experiencia, o un ADMIN (`403` en otro caso). |

**`POST /orders` hace:**
1. Busca el carrito del usuario (`404` si no tiene) y valida que **no esté vacío** (`400`).
2. Revalida `availableSeats` de cada sesión (`400` si no alcanza).
3. `subtotal` = Σ `precioExperiencia * cantidad`.
4. Si hay `couponCode`: lo busca (`404`), valida `active` + vigencia (`400`) y calcula
   `discountAmount = subtotal * percentage / 100` (2 decimales, `HALF_UP`).
5. `total = subtotal - discountAmount`.
6. Crea el `Order`, y **un `Booking` por cada item** con `voucherCode` `ETRIP-XXXXXXXX`.
7. **Descuenta** `availableSeats` de cada sesión.
8. **Vacía** el carrito.

## Archivos del módulo 4

| Archivo | |
|---|---|
| `service/impl/DiscountCouponServiceImpl.java` | Lógica de cupones |
| `service/impl/OrderServiceImpl.java` | Confirmación de reserva (checkout) |
| `service/impl/BookingServiceImpl.java` | Consulta de vouchers |
| `controllers/coupons/DiscountCouponsController.java` | Endpoints de cupones |
| `controllers/orders/OrdersController.java`, `controllers/bookings/BookingsController.java` | Endpoints de reservas / vouchers |

---

# Usuarios y Perfil

Base: `/users` · Requiere estar autenticado.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/users/me` | Mi perfil completo: `id`, `firstName`, `lastName`, `role`, `email`, y contadores `publishedExperiences` / `bookingsCount` / `reviewsCount`. |
| `PUT` | `/users/me` | Edita `firstName` / `lastName` (al menos uno, sino `400`). Email, contraseña y rol no se tocan acá. |
| `GET` | `/users/{id}` | Perfil de otro usuario. Público: `id`, nombre, `role`, `publishedExperiences`. El `email` y los contadores privados solo si sos vos mismo o un ADMIN. |
| `GET` | `/users?page=&size=` | **ADMIN:** lista de usuarios (`403` si no sos ADMIN). |
| `PATCH` | `/users/{id}/role?role=CLIENTE\|ADMIN` | **ADMIN:** asigna permisos. `400` rol inválido, `403` si intentás cambiarte tu propio rol. |

Cubre el requisito de la consigna "administración de cuentas de usuario, incluyendo la asignación
de permisos".

## Reseñas — endpoint extra

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/reviews/mine?page=&size=` | Reseñas escritas por el usuario autenticado. |

> `/reviews/**` es el módulo de Sofi. Este endpoint es un agregado read-only
> (`ReviewRepository.findByUserId` + `ReviewService.getMyReviews`).
