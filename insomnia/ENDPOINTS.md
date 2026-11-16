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
3. Correr **POST register** (pide `username` además de nombre/apellido/mail/contraseña): la cuenta
   queda **sin verificar** y llega un código al email. Después **POST verify-email** con ese código.
   Si ya tenés usuario verificado, **POST authenticate**. **No hace falta copiar el token a mano**: las
   requests de `01 - Auth` que devuelven token tienen un *After-Response Script* que guarda `access_token` en la variable `token`
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
| `GET /experiences/**`, `GET /experience-categories/**`, `GET /interests`, `GET /experience-sessions/**`, `GET /reviews/experience/**`, `GET /users/{id}` | **público** (el catálogo se ve sin sesión) |
| `GET /experiences/recommended` | CLIENTE o ADMIN (recomendaciones del usuario logueado) |
| `POST/PUT/DELETE /interests/**` | **solo ADMIN** |
| `GET /experiences/mine`, `GET /experience-sessions/manage`, `GET /experience-sessions/manage/summary` | CLIENTE o ADMIN (aunque estén bajo un prefijo público) |
| `POST/PUT/DELETE /experience-categories/**` | **solo ADMIN** |
| `/users/**` (resto) | CLIENTE o ADMIN (algunas acciones dentro, como `GET /users` o `PATCH .../role`, exigen ADMIN y devuelven 403 si no) |
| `/experiences`, `/experience-sessions` (escrituras) | CLIENTE o ADMIN; además solo el dueño de la experiencia o un ADMIN (403 si no) |
| `/reviews`, `/carts`, `/orders`, `/bookings`, `/notifications`, `/favorites` | CLIENTE o ADMIN |
| `/experience-sessions/{id}/waitlist`, `/experience-sessions/waitlist/mine` | CLIENTE o ADMIN (lista de espera) |
| `GET /discount-coupons/validate` | CLIENTE o ADMIN |
| Resto de `/discount-coupons/**` (listar, ver, crear, editar, borrar) | **solo ADMIN** |

- **`register` siempre crea rol `CLIENTE`** (el campo `role` del body, si lo mandás, se ignora).
- Para tener un ADMIN: `UPDATE user SET role='ADMIN' WHERE email='...';` en MySQL. El rol **no**
  viaja en el JWT (solo `sub`, `id`, `iat`, `exp`): se lee de la base en cada request, así que el
  cambio se aplica enseguida. También podés pedirle a otro ADMIN que te lo asigne con
  `PATCH /users/{id}/role?role=ADMIN`.
- Una cuenta desactivada (`PATCH /users/{id}/status?active=false`) no puede loguearse y su token
  deja de servir aunque no haya vencido (el filtro JWT chequea `isEnabled()`).
- CORS: los orígenes permitidos salen de `app.cors.allowed-origins` (por defecto
  `http://localhost:5173,http://localhost:3000`). Al desplegar el front, definir la variable de
  entorno `CORS_ALLOWED_ORIGINS` con la URL de Vercel.
- Fechas: formato ISO local sin zona → `2026-10-15T10:00:00`.

### Reglas que agregó la revisión de seguridad (19/10)

- `GET /users/{id}` ajeno/anónimo ya **no** devuelve `role` ni `active` (solo en `/users/me` o para ADMIN).
- `forgot-password` ya no devuelve el código: llega **por mail** (ver "Verificación de email y mails").
- `register`: el email tiene que tener formato de email (≤150), el username no puede tener `@` (≤50), nombre/apellido ≤100.
- Fotos de experiencias: hasta 5, de hasta 5 MB, JPG/PNG/WEBP de verdad (si no se decodifica → 400) y como mucho 40 megapíxeles.
- Los **listados** de experiencias (`GET /experiences`, `/mine`, búsquedas) mandan **solo la portada** en `imagesBase64`; `GET /experiences/{id}` manda todas.
- Descuento de experiencia: 0–90% (igual que el front). Título ≤150, descripción ≤2000, ubicación ≤200, reseña ≤1000, categoría ≤100 (descripción ≤255).
- `DELETE /experiences/{id}` (dar de baja) **reembolsa y avisa a todos** los que tenían reservas vigentes en fechas futuras, cancela esas fechas y avisa a la lista de espera. Si la experiencia nunca se vendió se borra del todo; si se vendió, queda en estado `DELETED` (solo la ve el ADMIN) para que las reservas viejas, reseñas y estadísticas no se pierdan. `DELETE` definitivo de una fecha: solo si ya terminó o está cancelada **y** nunca tuvo reservas.
- `PUT /experience-sessions/{id}`: con reservas vigentes no se puede cambiar el horario (sí el cupo).
- `Booking.unitPrice` guarda el precio pagado; voucher, reembolsos y stats lo usan (las reservas viejas usan el precio actual).
- `POST /orders` acepta `expectedTotal` (opcional): si no coincide con el total calculado, 400 sin cobrar.
- Reembolso, cancelar/editar fecha y cupón en la compra bloquean las filas (doble clic ya no duplica lugares ni usos de cupón).
- `?page=-1` / `?size=0` y datos que la base no acepta → 400 (antes 500). `%` y `_` en la búsqueda se buscan literal.

---

## 1. Auth — `/api/v1/auth` (público)

### Verificación de email y mails

Para crear una cuenta hay que **probar que el email es tuyo**: el registro manda un código de 6 dígitos y
recién con ese código se entrega el token. Los mails salen de `equipodeplan@gmail.com` (Gmail con una
contraseña de aplicación). Las credenciales van en `eTrip back/.env` (**no** se sube a git; copiar `.env.example`):

```
MAIL_USERNAME=equipodeplan@gmail.com
MAIL_PASSWORD=<contraseña de aplicación de 16 letras, sin espacios>
```

Si `MAIL_USERNAME` está vacío, el back no manda nada y escribe el mail (con el código en el asunto) en su consola.
Si Gmail rechaza el envío → `503` "No pudimos enviar el email…" y no se guarda nada (se puede reintentar).

Los códigos pendientes se guardan en la tabla `verification_codes` (`purpose` = `VERIFY_EMAIL` o
`RESET_PASSWORD`; como mucho uno de cada tipo por usuario): solo el **hash** del código, vence en **15 minutos**,
con **5 intentos** se invalida y **no se manda otro mail a la misma cuenta antes de 1 minuto** (mientras tanto sigue
valiendo el último). La tabla vieja `password_reset_codes` se borra sola al arrancar (`LegacyColumnsCleanup`).
Las cuentas que ya existían antes de este cambio y las del seed quedan verificadas (`user.email_verified`).

### POST `/api/v1/auth/register`
```json
{
  "username": "nachoc",
  "firstname": "Nacho",
  "lastname": "Cortes",
  "email": "nacho@etrip.com",
  "password": "nacho1234",
  "interests": ["Gastronomía", "Vinos"]
}
```
Respuesta: `{ "email": "nacho@etrip.com" }` (**sin token**): la cuenta queda sin verificar y le llega el código.
`interests` es opcional (máx. 20): **nombres de intereses que existan** en `GET /interests` (si no, `400`
"“X” no es un interés de PLAN"). Con ellos el back arma "Para vos". `username` y `email` son únicos. Si falta cualquier campo → `400`. Si el username o el email ya existen → `400` con
`"Ese nombre de usuario ya está en uso"` / `"Ya hay una cuenta con ese email"`.

### POST `/api/v1/auth/verify-email`
`{ "usernameOrEmail": "nacho@etrip.com", "code": "123456" }` → `{ "access_token": "eyJ..." }` (queda logueado).
`400` si el código no es correcto, venció o se agotaron los intentos.

### POST `/api/v1/auth/resend-verification`
`{ "usernameOrEmail": "nacho@etrip.com" }` → `204` siempre (si la cuenta no existe o ya está verificada no hace
nada: no le confirma a nadie qué cuentas existen).

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
Respuesta: `{ "access_token": "eyJ..." }`. `401` si los datos no son correctos o la cuenta está desactivada.
**`403` si la contraseña es correcta pero el email no está verificado:** el back le manda un código nuevo y el
front pasa a la pantalla de verificar.

### GET `/api/v1/auth/availability?username=nachoc&email=nacho@etrip.com`
Los dos parámetros son opcionales. Respuesta: `{ "usernameAvailable": false, "emailAvailable": true }`
(`null` en el que no se mandó). El registro del front lo usa para avisar antes de seguir.

### Recuperar contraseña (3 pasos)
Usa la misma tabla `verification_codes` (`purpose = RESET_PASSWORD`) y las mismas reglas de arriba.

1. **POST `/api/v1/auth/forgot-password`** — `{ "usernameOrEmail": "nacho@etrip.com" }` → `204`.
   Manda **por mail** un código de 6 dígitos que vence en 15 minutos; pedirlo de nuevo (pasado el minuto) anula el anterior.
   `404` si la cuenta no existe, `400` si está desactivada.
2. **POST `/api/v1/auth/verify-reset-code`** — `{ "usernameOrEmail": "...", "code": "123456" }` →
   `204` si es válido, `400` si no. No cambia nada. Con 5 intentos fallidos el código se invalida.
3. **POST `/api/v1/auth/reset-password`** — `{ "usernameOrEmail": "...", "code": "123456", "newPassword": "nueva123" }`
   → `{ "access_token": "eyJ..." }` (queda logueado). Mínimo 6 caracteres. Si la cuenta no estaba verificada,
   **queda verificada** (el código llegó a su mail, así que el email es suyo).

---

## 2. Usuarios y Perfil — `/users`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/users/me` | propio | Mi perfil: `username`, nombre, `role`, `email`, `bio`, `city`, `avatarBase64`, `interests`, `publishedExperiences`, `bookingsCount`, `reviewsCount`. |
| PUT | `/users/me` | propio | Editar nombre, apellido, `bio`, `city`, foto (`avatarBase64`) e `interests`. Todo opcional. |
| GET | `/users/{userId}` | público | Perfil público de otro usuario (sin email/contadores privados salvo que seas vos o ADMIN). Sin sesión también responde (perfil reducido). |
| GET | `/users?page=&size=` | **ADMIN** | Lista de usuarios. |
| PATCH | `/users/{userId}/role?role=CLIENTE\|ADMIN` | **ADMIN** | Asigna permisos. No podés cambiarte tu propio rol (403). |

### PUT `/users/me`
```json
{
  "firstName": "Nacho",
  "lastName": "Cortes",
  "bio": "Me gusta descubrir mi ciudad como si fuera turista.",
  "city": "Buenos Aires",
  "avatarBase64": "data:image/jpeg;base64,/9j/4AAQ...",
  "interests": ["Gastronomía", "Vinos", "Música en vivo"]
}
```
Un campo que no se manda (o `null`) no se toca; un string vacío (`""`) lo borra (la foto también). `bio` hasta 500
caracteres, `city` 100, la foto hasta 400 KB (data URL o base64 pelado), `interests` hasta 20 (nombres de `GET /interests`).
El perfil público (`GET /users/{id}`) muestra `bio`, `city`, `avatarBase64` e `interests` de cualquier usuario:
es la ficha del anfitrión en la página de la experiencia.

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
`DELETE` → `400` si la categoría tiene experiencias **o intereses** asociados.

---

## 3 bis. Intereses — `/interests`

Los intereses son una **entidad** (tabla `interests`): cada uno pertenece a una categoría (el grupo con el que se
muestra) y se relaciona con los usuarios que lo eligieron (`user_interest`) y con las experiencias que lo tienen
(`experience_interest`). Los administra el ADMIN desde el panel. El seed carga 27.

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/interests` | público | Todos, ordenados por categoría y nombre: `{ id, name, categoryId, categoryName, availableExperiences }`. `availableExperiences` = cuántas experiencias del catálogo público con alguna fecha con lugar lo tienen (lo calcula el back). |
| POST | `/interests` | **ADMIN** | `{ "name": "Escalada", "categoryId": 2 }` → `201`. Nombre único (`400` si se repite), hasta 40 caracteres. |
| PUT | `/interests/{id}` | **ADMIN** | Mismo body. Renombrar se ve enseguida en experiencias y perfiles (apuntan al interés, no al texto). |
| DELETE | `/interests/{id}` | **ADMIN** | `204`. Se lo saca a las experiencias y usuarios que lo tenían. |

En los requests y respuestas de usuarios y experiencias, `interests` / `tags` siguen siendo **listas de nombres**.

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
| PATCH | `/experiences/{experienceId}/status?active=true\|false` | — (pausar / volver a publicar; solo el dueño o ADMIN) |
| DELETE | `/experiences/{experienceId}` | — (dar de baja: reembolsa y avisa a todos, ver abajo) |

### Filtros de `GET /experiences` (todos opcionales, se combinan con AND)

| Param | Efecto |
|---|---|
| `categoryId` | Categoría exacta. `404` si no existe. |
| `title` | Cada palabra tiene que estar en el título, el lugar o el nombre del anfitrión (sin importar mayúsculas ni acentos). |
| `location` | Coincidencia parcial en la ubicación. |
| `minPrice` / `maxPrice` | Rango sobre el **precio final** (con descuento). `minPrice > maxPrice` → `400`. |
| `publisherId` | Experiencias de un anfitrión puntual. |
| `onlyDiscounted` | Solo con descuento de producto activo. |
| `dateFrom` / `dateTo` | Días (`2027-01-31`): alguna fecha que empiece en ese rango. |
| `guests` | Alguna fecha con al menos esos lugares libres. |
| `onlyAvailable` | Alguna fecha con lugares libres. |
| `durationMin` / `durationMax` | Duración de la fecha, en minutos. `durationMin > durationMax` → `400`. |
| `timeSlots` | Franja de inicio, separadas por coma: `manana` (antes de las 12), `tarde` (12 a 17), `noche` (desde las 17). |
| `withKids` | `true` = el grupo tiene niños o bebés: no muestra las experiencias solo para mayores de 18. |
| `tags` | Intereses, separados por coma (`tags=Vinos,Gastronomía`): experiencias que tengan **alguno** de esos intereses (por nombre). |

Fechas, personas, duración y horario se cumplen en **una misma fecha**. El catálogo público muestra solo experiencias publicadas con alguna fecha futura. Es toda la búsqueda del Home: el front solo ordena.

Ejemplo: `/experiences?location=Palermo&minPrice=1000&maxPrice=50000&dateFrom=2027-01-01&dateTo=2027-12-31&guests=2&timeSlots=tarde,noche&withKids=true`

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
  "categoryId": 1,
  "tags": ["Historia", "Caminata", "Arquitectura"]
}
```
`tags` (opcional, hasta 8): nombres de intereses de `GET /interests` (`400` si alguno no existe); la respuesta los
devuelve en `tags` y además trae `favoriteCount` (cuántas personas la guardaron).

### GET `/experiences/recommended?viewed=12,5` — "Para vos" (con sesión)
Devuelve hasta 8 `[{ "experience": { …como en los listados… }, "reason": "Porque te gusta vinos y gastronomía" }]`.
Lo calcula el back con un **perfil de gustos**: cada experiencia del catálogo con fecha disponible suma puntos por
cada interés que comparte con los intereses del usuario (x3), con lo que guardó o reservó (x2) y con lo que vio
hace poco (x1; `viewed` = ids que manda el front, porque "vistos" vive en el navegador). No se recomiendan las
propias, las guardadas ni las que ya reservó. Desempata el promedio de reseñas. `reason` es lo que más sumó:
"Porque te gusta…", "Parecida a “…”, que guardaste", "Porque fuiste a “…”", "Parecida a “…”, que reservaste",
"Porque viste “…”". Sin sesión → `403`.

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
> **publisher** o un **ADMIN** pueden editar/borrar.

### DELETE `/experiences/{id}` — dar de baja con reembolso a todos
Regla de PLAN: si el anfitrión cancela la experiencia, **nadie se queda sin su plata ni sin aviso**. En una sola transacción:
1. Por cada fecha futura activa: bloquea la fecha, reembolsa todas las reservas vigentes (quedan `refunded`, los lugares vuelven),
   avisa por notificación al comprador (y a quien recibió un regalo) y a la lista de espera, y cancela la fecha.
2. Saca la experiencia de los carritos y de los guardados de todos.
3. Si **nunca se vendió**, se borra del todo (fechas, reseñas, lista de espera). Si se vendió alguna vez, queda
   `active=false` con `deletedAt`: desaparece del catálogo y de "Mis experiencias", da `404` al abrirla (salvo ADMIN, que
   la ve con `status: "DELETED"`), pero las reservas viejas, reseñas y estadísticas siguen existiendo.

Devuelve `200` con el resumen, para que el front lo muestre:
```json
{ "experienceId": 12, "removed": false, "cancelledSessions": 3, "refundedBookings": 7 }
```

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
| GET | `/experience-sessions/manage` | — **(logueado)** fechas que gestiono, **incluidas pasadas y canceladas**: ADMIN recibe todas; cualquier otro usuario, las de sus experiencias. Lista simple (no paginada), ordenada por `startsAt`. |
| GET | `/experience-sessions/manage/summary` | — **(logueado)** "Fechas y huéspedes" del anfitrión: una fila por cada experiencia suya (también sin fechas) con `nextSessionAt`, `upcomingSessions` / `finishedSessions` / `cancelledSessions`, y de las próximas `bookedSeats`, `capacity`, `occupancyPercent` y `waitlistCount`. Primero las que tienen la próxima fecha más cerca. |
| GET | `/experience-sessions/{sessionId}` | — |
| POST | `/experience-sessions` | ver abajo |
| PUT | `/experience-sessions/{sessionId}` | ver abajo (campos opcionales) |
| DELETE | `/experience-sessions/{sessionId}` | — (cancela la fecha: ver abajo) |
| GET | `/experience-sessions/waitlist/mine` | — **(logueado)** ids de las fechas en las que estoy en lista de espera. |
| POST | `/experience-sessions/{sessionId}/waitlist` | — "Avisame si se libera un lugar": solo en fechas **agotadas**, activas y futuras, de otra persona (`400` si no). Idempotente. |
| DELETE | `/experience-sessions/{sessionId}/waitlist` | — Me bajo de la lista. |

**Lista de espera:** cada fecha devuelve `waitlistCount` (cuántos esperan). Cuando se libera un lugar (un reembolso, o el
anfitrión amplía el cupo con `PUT`), el back **notifica a todos los anotados** ("¡Se liberó un lugar!") y los saca de la
lista: se avisa una sola vez. Si la fecha se cancela, también se les avisa.

> Los listados públicos (`GET /experience-sessions` y `/experience/{id}`) solo devuelven sesiones
> **activas y futuras** de experiencias activas. Para el historial del anfitrión usar `/manage`.

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
> Una sesión no se puede pasar a otra experiencia: si `experienceId` viene y es distinto al actual → `400`.
> `capacity` no puede quedar por debajo de los lugares ya reservados.

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
> `capacity`. **`DELETE` no borra:** cancela la sesión (`active=false`), reembolsa todas sus reservas
> y les manda una notificación a los compradores; una sesión que ya empezó no se puede cancelar (`400`).
> Si no hay `availableSeats`, no se puede agregar al carrito (lo valida el módulo de Carrito).
> **`POST`/`PUT`/`DELETE` son solo para el dueño (publisher) de la experiencia o un `ADMIN`**
> (`403` en otro caso) — los `GET` públicos se ven sin sesión.

---

## 6. Reseñas — `/reviews`

| Método | Ruta | Body |
|---|---|---|
| GET | `/reviews?page=0&size=10` | — **CLIENTE: solo las propias. ADMIN: todas.** |
| GET | `/reviews/experience/{experienceId}?page=0&size=10` | — **pública**, cualquier logueado (son la opinión sobre un producto del catálogo, no datos privados) |
| GET | `/reviews/mine?page=0&size=10` | — mis reseñas (igual que `GET /reviews` para un CLIENTE, queda como alias explícito) |
| GET | `/reviews/{reviewId}` | — **CLIENTE: solo si es autor (`403` si no). ADMIN: cualquiera.** |
| POST | `/reviews` | ver abajo |
| PUT | `/reviews/{reviewId}/reply` | `{ "reply": "¡Gracias por venir!" }` — **solo el anfitrión de la experiencia o ADMIN**. Crea o edita la respuesta pública; `reply` vacío la borra. Avisa al autor con una notificación. |
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
| GET | `/orders/stats` | — **solo ADMIN**: `{ orderCount, grossSales, refunded, netSales, commission, commissionRate }`. La comisión de PLAN es el 10% de las ventas netas (cobrado menos reembolsos). |
| GET | `/orders/{orderId}` | — |
| POST | `/orders` | opcional, ver abajo |

### POST `/orders` (checkout)
Toma **el carrito del usuario autenticado**, calcula el subtotal usando el **precio con descuento**
de cada experiencia (si tiene), crea la orden + una `Booking` con voucher por cada item, descuenta
`availableSeats` y vacía el carrito. Body opcional:
```json
{
  "couponCode": "VERANO2026",
  "paymentMethod": "card",
  "cardNumber": "4242424242424242",
  "gift": { "recipientName": "Sofía Pérez", "recipientEmail": "sofia@plan.com", "message": "¡Feliz cumple!" }
}
```
`couponCode` es opcional. `gift` también: si viene, la compra es un **regalo**. `recipientName` es obligatorio (hasta 100),
`recipientEmail` opcional: con email, el back le manda un **mail de aviso** (experiencia, fecha, lugar, personas y
dedicatoria; **sin el voucher**, así un email mal escrito no le sirve a nadie). El regalo **pasa a ser de quien lo recibe
solo cuando tiene una cuenta verificada con ese email**: si ya la tiene, en el momento (lo ve en **sus** "Mis reservas",
recibe "¡Te regalaron una experiencia!" y puede reseñar); si no, queda **pendiente** y se le entrega cuando se registra
y verifica ese email ("¡Tenés un regalo esperándote!"). Mientras está pendiente, el voucher es de quien pagó, que puede:
- `PATCH /orders/{id}/gift` `{ "recipientEmail": "bien@escrito.com" }` → corrige el email y reenvía el aviso (si es de una
  cuenta verificada, se entrega ya). `400` si ya se entregó o ya no está vigente; `403` si no es quien pagó.
- `DELETE /orders/{id}/gift` → se lo queda: deja de ser un regalo.

La orden y cada reserva traen `giftStatus`: `PENDING`, `DELIVERED` o `IN_PERSON` (sin email: se entrega en mano).
Si el mail no se puede mandar al comprar, la compra no se deshace: el comprador recibe un aviso. `message` hasta 300. La orden y
cada reserva devuelven `giftRecipientName`, `giftMessage`, `giftFromName` y `recipientId`. **Pago simulado** (no hay cobro real): `paymentMethod` es `card` (tarjeta nueva,
requiere `cardNumber` válido por Luhn), `saved` (tarjeta guardada) o una billetera (`mercadopago`, `paypal`,
`gpay`); sin `paymentMethod` se aprueba directo (útil en Insomnia). La tarjeta de prueba `4000 0000 0000 0002`
siempre es rechazada (`400` "El banco rechazó la tarjeta…") y en ese caso no se toca nada. Cada orden devuelve
también `refunded` y `commission` (10% de PLAN).

> Requiere que el carrito tenga items. Todo pasa en una transacción: si algo falla, no se descuenta
> nada. Antes de validar, **bloquea las sesiones del carrito** (`SELECT ... FOR UPDATE`) y las vuelve
> a leer: si dos personas compran el último lugar a la vez, la segunda espera y recibe `400` por
> falta de cupos (no se sobrevende). Valida también que cada sesión siga activa y sea futura, y que
> la experiencia siga publicada. El `couponCode` se normaliza a mayúsculas y se valida igual que
> `GET /discount-coupons/validate` (incluido que no lo hayas usado antes); si no sirve, el `400`
> trae el motivo en `message` ("Este cupón venció", "Ya usaste este cupón en otra compra"…).

---

## 9. Reservas — `/bookings`

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/bookings?page=0&size=10` | Mis vouchers: las que compré **y las que me regalaron** (ADMIN ve todas). |
| GET | `/bookings/sales?page=0&size=10` | **Modo vendedor:** reservas sobre las experiencias que publiqué, con `buyerId`/`buyerName`. |
| GET | `/bookings/experience/{experienceId}?page=0&size=10` | Reservas de una experiencia puntual. Solo el dueño de esa experiencia o ADMIN (403/404 si no). |
| GET | `/bookings/{bookingId}` | Una reserva. La ve el comprador, el vendedor de esa experiencia, o ADMIN. |
| POST | `/bookings/{bookingId}/refund` | Cancela la reserva con reembolso y devuelve los lugares a la sesión. Ver política abajo. |

Las reservas se generan en el checkout (`POST /orders`).

**Política de cancelación (`refund`):** el **comprador** puede cancelar hasta **48 h antes** de que
empiece la sesión (después → `400` con el motivo). El **anfitrión** de la experiencia y un **ADMIN**
pueden cancelar hasta el inicio (por ejemplo, si se suspende). Una sesión que ya empezó no se reembolsa.
Una reserva reembolsada no habilita a dejar reseña. Al reembolsar se avisa a la lista de espera de esa fecha y, si
la reserva era un regalo, también a quien lo recibió ("Regalo cancelado"; la plata vuelve a quien pagó). Quien recibe un
regalo ve la reserva pero **no** puede pedir el reembolso ni ver la orden (son de quien compró).

---

## 9 bis. Guardados — `/favorites`

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/favorites` | Mis guardados: `[{ experienceId, createdAt }]`, el más reciente primero. |
| POST | `/favorites/{experienceId}` | Guardar (idempotente). `400` si es mi propia experiencia o no está publicada. |
| DELETE | `/favorites/{experienceId}` | Quitar. `204`. |

Las experiencias devuelven `favoriteCount`. Si una experiencia se da de baja, sale de los guardados de todos.

---

## 10. Cupones — `/discount-coupons`

| Método | Ruta | Permiso | Body |
|---|---|---|---|
| GET | `/discount-coupons?page=0&size=10` | ADMIN | — |
| GET | `/discount-coupons/validate?code=` | CLIENTE/ADMIN | — (chequeo antes de pagar, ver abajo) |
| GET | `/discount-coupons/{couponId}` | ADMIN | — |
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
`INVALID_PERCENTAGE`, `ALREADY_USED` (el usuario logueado ya lo usó en otra orden: cada cliente puede
usar un cupón una sola vez). Pensado para que el front lo llame apenas el usuario tipea el código,
antes de tocar `POST /orders`.

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
14. `POST /favorites/{id}` (comprador) y `GET /favorites`; `POST /experience-sessions/{id}/waitlist` en una fecha agotada,
    `POST /bookings/{id}/refund` de otra reserva de esa fecha y mostrar la notificación "¡Se liberó un lugar!".
15. `POST /orders` con `gift` → el destinatario ve la reserva en `GET /bookings`.
16. (vendedor) `DELETE /experiences/{id}` → todos reembolsados y avisados; el ADMIN la sigue viendo con `status: "DELETED"`.
