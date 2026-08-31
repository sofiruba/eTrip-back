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
| 2 | Sesiones / Turnos | Stubs |
| 3 | Carrito | Stubs |
| 4 | Reservas y Descuentos | Stubs |

Login y registro ya venían resueltos en el proyecto base.

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
| `GET` | `/experiences?page=&size=` | Lista paginada, con `imageBase64`. |
| `GET` | `/experiences/{id}` | Experiencia por id. `404` si no existe. |
| `POST` | `/experiences` | Crea. `multipart/form-data`: parte `experience` (JSON) + parte `image` (archivo, **obligatoria**). |
| `PUT` | `/experiences/{id}` | Actualiza. `multipart/form-data`: `experience` (JSON) + `image` (opcional; si no viene, mantiene la actual). Solo el **dueño** o un **ADMIN**. |
| `DELETE` | `/experiences/{id}` | Elimina. Solo el **dueño** o un **ADMIN**. `400` si tiene sesiones asociadas. |

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
