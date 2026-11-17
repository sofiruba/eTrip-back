# PLAN · Endpoints

Base: `http://localhost:4002`. Salvo `/api/v1/auth/**` y los GET públicos, todo pide `Authorization: Bearer <token>`.
Los listados se pueden paginar con `?page=&size=` (sin esos parámetros traen todo).

**Errores:** siempre `{ "status": 400, "message": "…" }`, con un mensaje en castellano que el front muestra tal cual.
Una lista vacía responde `200 []`; 404 es solo para algo pedido por id que no existe.

| Código | Cuándo |
|---|---|
| 400 | Regla de negocio o dato inválido ("Tu carrito está vacío", "No quedan lugares…") |
| 401 | Sin sesión o token vencido; login incorrecto o cuenta desactivada |
| 403 | Sin permiso para ese recurso, o email todavía sin verificar (en el login) |
| 404 | El recurso pedido por id no existe |
| 503 | No se pudo mandar un mail (no se guarda nada; se puede reintentar) |

**Cómo usar la colección de Insomnia** (`eTrip_insomnia.json`): importarla, elegir el environment "Local" y correr
un login de la carpeta `01 - Auth`. Un script guarda el token en `token` y el id del usuario en `user_id`; el resto de
los pedidos los usan solos. Hay logins listos para la compradora de demo, un anfitrión y el admin.

---

## 1. Auth — `/api/v1/auth` (público)

| Método | Ruta | Body → respuesta |
|---|---|---|
| POST | `/register` | `{ username, firstname, lastname, email, password, interests? }` → `{ email }`. La cuenta queda **sin verificar** y llega un código al mail |
| POST | `/verify-email` | `{ usernameOrEmail, code }` → `{ access_token }` |
| POST | `/resend-verification` | `{ usernameOrEmail }` → 204 (responde igual exista o no la cuenta) |
| POST | `/authenticate` | `{ usernameOrEmail, password }` → `{ access_token }`. **403** si el email no está verificado (manda un código nuevo) |
| GET | `/availability?username=&email=` | `{ usernameAvailable, emailAvailable }` (lo usa el registro antes de avanzar) |
| POST | `/forgot-password` | `{ usernameOrEmail }` → 204; manda un código al mail. 404 si la cuenta no existe |
| POST | `/verify-reset-code` | `{ usernameOrEmail, code }` → 204 si es válido |
| POST | `/reset-password` | `{ usernameOrEmail, code, newPassword }` → `{ access_token }`. También deja el email verificado |

Reglas: usuario sin `@` (≤ 50), email válido y único, contraseña ≥ 6, nombre y apellido ≤ 100, intereses existentes
(≤ 20). Los códigos vencen a los 15 min, se invalidan con 5 intentos y no se manda otro mail antes de 1 min.

## 2. Usuarios — `/users`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/users/me` | logueado | Mi perfil con contadores (`bookingsCount`, `reviewsCount`, `publishedExperiences`) |
| PUT | `/users/me` | logueado | `{ firstName?, lastName?, bio?, city?, avatarBase64?, interests? }`: solo lo que cambia; en bio, ciudad y foto `""` lo borra |
| GET | `/users/{id}` | público | Perfil público (sin email ni datos privados) |
| GET | `/users` | ADMIN | Todos los usuarios |
| PATCH | `/users/{id}/role?role=ADMIN\|CLIENTE` | ADMIN | No se puede cambiar el propio |
| PATCH | `/users/{id}/status?active=false` | ADMIN | Desactivar o reactivar. Una cuenta desactivada no entra y sus experiencias salen del catálogo. Al reactivarla recibe los regalos pendientes |

## 3. Catálogo

### Categorías — `/experience-categories`
GET (público, con `experienceCount`) · POST / PUT `{ name, description }` · DELETE (ADMIN). No se borra una
categoría con experiencias o intereses.

### Intereses — `/interests`
| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/interests` | público | `{ id, name, categoryId, categoryName, availableExperiences }`, ordenados por categoría |
| POST / PUT | `/interests`, `/interests/{id}` | ADMIN | `{ name, categoryId }` (nombre único) |
| DELETE | `/interests/{id}` | ADMIN | Se lo saca a experiencias y usuarios |

### Monedas — `/currencies`
GET (público): `[{ code, name, symbol, arsPerUnit, updatedAt }]`, primero los pesos. PUT `/currencies/{code}`
`{ arsPerUnit }` (ADMIN) cambia una cotización; los pesos siempre valen 1.

### Experiencias — `/experiences`
| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/experiences` | público | Catálogo con filtros: `title`, `location`, `categoryId`, `minPrice`/`maxPrice` (precio final), `onlyDiscounted`, `dateFrom`/`dateTo`, `guests`, `onlyAvailable`, `durationMin`/`durationMax`, `timeSlots=manana,tarde,noche`, `withKids`, `tags=Vinos,Café`. Los listados mandan solo la foto de portada |
| GET | `/experiences/{id}` | público | Detalle con todas las fotos |
| GET | `/experiences/mine` | logueado | Las mías (también pausadas) |
| GET | `/experiences/recommended?viewed=12,5` | logueado | "Para vos": hasta 8 `{ experience, reason }`. Puntúa con mis intereses (x3), lo que guardé o reservé (x2) y lo que vi (x1); sin las mías, guardadas ni reservadas |
| POST | `/experiences` | logueado | `multipart/form-data`: parte `experience` (JSON: `title, description, price, location, categoryId, adultsOnly, tags`) + 1 a 5 `images` (≤ 5 MB) |
| PUT | `/experiences/{id}` | dueño o ADMIN | Igual que POST; sin `images`, las fotos quedan como estaban |
| PATCH | `/experiences/{id}/discount` | dueño o ADMIN | `{ discountPercentage }`, de 0 a 90 % |
| PATCH | `/experiences/{id}/status?active=false` | dueño o ADMIN | Pausar / publicar de nuevo |
| DELETE | `/experiences/{id}` | dueño o ADMIN | Dar de baja: cancela sus fechas futuras, reembolsa y avisa a todos. Si nunca se vendió se borra; si no, queda `DELETED`. Devuelve el resumen (`cancelledSessions`, `refundedBookings`) |

`status` de cada experiencia: `ACTIVE`, `NO_DATES` (sin fechas futuras), `INACTIVE` (pausada) o `DELETED`.

### Fechas — `/experience-sessions`
| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/experience-sessions` | público | Fechas futuras de experiencias publicadas (`onlyAvailable`, `dateFrom`, `dateTo`). Cada una trae `refundableUntil` (48 h antes) |
| GET | `/experience-sessions/experience/{id}` | público | Las de una experiencia |
| GET | `/experience-sessions/{id}` | público | Una fecha |
| GET | `/experience-sessions/manage` | logueado | Las mías (ADMIN: todas), también pasadas y canceladas, con `status` (`PUBLISHED`, `SOLD_OUT`, `FINISHED`, `CANCELLED`), `editable` y `waitlistCount` |
| GET | `/experience-sessions/manage/summary` | logueado | "Fechas y huéspedes": una fila por experiencia mía con fechas por estado, próxima fecha, lugares reservados, ocupación y lista de espera |
| POST | `/experience-sessions` | dueño o ADMIN | `{ experienceId, startsAt, endsAt, capacity }` (fecha futura, sin superponerse) |
| PUT | `/experience-sessions/{id}` | dueño o ADMIN | Con reservas no se cambia el horario ni se baja el cupo por debajo de lo vendido |
| DELETE | `/experience-sessions/{id}` | dueño o ADMIN | Cancelar (baja lógica): reembolsa y avisa a todos |
| DELETE | `/experience-sessions/{id}/permanent` | dueño o ADMIN | Borrar una fecha cancelada o terminada que nunca tuvo reservas |
| GET | `/experience-sessions/waitlist/mine` | logueado | Ids de las fechas agotadas en las que estoy anotado |
| POST / DELETE | `/experience-sessions/{id}/waitlist` | logueado | Anotarme / bajarme de la lista de espera (solo fechas agotadas) |

## 4. Compra

### Carrito — `/carts`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/carts/user/{userId}` | Mi carrito con total (vacío: `200` con `items: []` y `total: 0`). Saca solo las fechas que ya no se pueden comprar |
| POST | `/carts/items` | `{ experienceSessionId, quantity }` (el usuario sale del token). No se puede agregar una experiencia propia, agotada o pasada |
| PATCH | `/carts/user/{userId}/items/{itemId}?quantity=2` | Cambiar la cantidad |
| DELETE | `/carts/user/{userId}/items/{itemId}` · `/carts/user/{userId}` | Quitar un ítem · vaciar |

### Cupones — `/discount-coupons`
| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/discount-coupons/validate?code=BIENVENIDA15` | logueado | `{ valid, message, percentage, subtotal, discountAmount, total }` con mi carrito |
| GET | `/discount-coupons`, `/{id}` | ADMIN | Con `status` (`ACTIVE`, `NOT_YET_VALID`, `EXPIRED`, `INACTIVE`) y `usedCount` |
| POST / PUT | `/discount-coupons`, `/{id}` | ADMIN | `{ code, percentage, validFrom, validUntil, active }` |
| DELETE | `/discount-coupons/{id}` | ADMIN | 204 si se borró; 200 con el cupón desactivado si ya se había usado |

### Órdenes — `/orders`
| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| POST | `/orders` | logueado (no ADMIN) | Checkout. Body abajo |
| GET | `/orders`, `/orders/{id}` | dueño (ADMIN: todas) | Con `refunded`, `commission` (lo que le queda a PLAN), `status` (`PAID`, `PARTIALLY_REFUNDED`, `CANCELLED`) y `giftStatus` |
| GET | `/orders/stats` | ADMIN | Ventas, reembolsos, ventas netas, comisión de PLAN y `commissionRate` |
| POST | `/orders/{id}/cancel` | ADMIN | Reembolsa todas sus reservas que todavía no empezaron |
| PATCH | `/orders/{id}/gift` | quien pagó | Regalo pendiente: `{ recipientEmail }` corrige el email y reenvía el aviso (si es de una cuenta verificada, se entrega ya) |
| DELETE | `/orders/{id}/gift` | quien pagó | Regalo pendiente: me lo quedo (deja de ser un regalo) |

```json
{
  "couponCode": "BIENVENIDA15",
  "paymentMethod": "card",
  "cardNumber": "4242 4242 4242 4242",
  "expectedTotal": 55250,
  "gift": { "recipientName": "Tomás", "recipientEmail": "tomas@plan.com", "message": "¡Feliz cumple!" }
}
```
Todo es opcional salvo el pago con tarjeta (`paymentMethod: "card"` valida el número; `4000 0000 0000 0002` se
rechaza siempre). Si `expectedTotal` no coincide con el total calculado (cambió un precio), 400 sin cobrar.
Regalo: se entrega solo a una cuenta **verificada** con ese email; si no, queda `PENDING` hasta que la cree y la
verifique. Sin email es `IN_PERSON` (el voucher se entrega en mano). No puede ser el email propio.

### Reservas (vouchers) — `/bookings`
| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/bookings` | logueado | Las que compré y las que me regalaron (ADMIN: todas) |
| GET | `/bookings/sales` | logueado | Reservas de mis experiencias (para el anfitrión) |
| GET | `/bookings/experience/{id}` | dueño o ADMIN | Reservas de una experiencia |
| GET | `/bookings/{id}` | quien compró, quien la recibió, el anfitrión o ADMIN | Una reserva |
| GET | `/bookings/{id}/voucher` | quien tiene el voucher | El voucher como imagen PNG |
| POST | `/bookings/{id}/refund` | quien tiene el voucher (hasta 48 h antes), el anfitrión o ADMIN (hasta que empiece) | Cancela y reembolsa a quien pagó |

Cada reserva trae, para quien la mira: `paidAmount`, `hostEarnings` (precio de lista menos el 10 %),
`commission` (lo que le queda a PLAN), `refundable` / `refundableUntil`, `reviewable` y, si es un regalo,
`giftRecipientName`, `giftFromName`, `giftMessage` y `giftStatus`. Quien pagó un regalo ya entregado recibe
`voucherCode: null`.

## 5. Después de la experiencia

### Reseñas — `/reviews`
| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/reviews/experience/{id}` | público | Reseñas de una experiencia (404 si la experiencia no existe; `[]` si no tiene) |
| GET | `/reviews/mine`, `/reviews` | logueado | Las mías (ADMIN: todas en `/reviews`) |
| POST | `/reviews` | quien fue | `{ experienceId, rating (1-5), comment }`. Una por experiencia, cuando ya terminó |
| PUT | `/reviews/{id}/reply` | anfitrión | `{ reply }`; vacío borra la respuesta |
| DELETE | `/reviews/{id}` | autor o ADMIN | |

### Guardados — `/favorites`
GET (ids que guardé) · POST `/favorites/{experienceId}` · DELETE `/favorites/{experienceId}`. No se puede guardar
una experiencia propia.

### Avisos — `/notifications`
GET (los míos, el más nuevo primero) · PATCH `/notifications/{id}/read`.
