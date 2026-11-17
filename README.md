# PLAN · API (back)

API REST de **PLAN**, un marketplace de experiencias con gente local: cualquiera puede publicar un plan
(una caminata, una clase de cocina, una peña) con fechas y cupos, y cualquiera puede reservarlo.

- **Stack:** Java 17, Spring Boot 3.1 (Web, Data JPA, Security, Mail), JWT, MySQL 8, Lombok.
- **Arquitectura:** `controllers` (HTTP) → `service` (reglas de negocio) → `repository` (JPA) → `entity`. Los datos
  viajan con DTOs (`dtos/request`, `dtos/response`) y los errores con un formato único `{ status, message }`.
- **Front:** carpeta `eTrip-front` (React + Redux Toolkit).
- **Todos los endpoints**, con ejemplos: [`insomnia/ENDPOINTS.md`](insomnia/ENDPOINTS.md). Colección para Insomnia:
  [`insomnia/eTrip_insomnia.json`](insomnia/eTrip_insomnia.json).

## Cómo levantarlo

1. **MySQL 8** corriendo en `localhost:3306` (la base `etrip_db` se crea sola).
2. **Configuración local:** copiar `.env.example` como `.env` y completarlo. `.env` no se sube a git.
   - `DB_PASSWORD` (y `DB_USER`, si no es `root`): el usuario de MySQL.
   - `JWT_SECRET`: una clave larga y al azar para firmar los tokens (por ejemplo, `openssl rand -hex 64`).
   - `MAIL_USERNAME` / `MAIL_PASSWORD`: una cuenta de Gmail con contraseña de aplicación. Si se deja vacío, los
     mails no se mandan y su asunto (que lleva el código de verificación) se escribe en la consola del back.
3. **Cargar los datos de ejemplo** (la primera vez; **borra todo lo que haya en la base**):
   ```bash
   ./mvnw spring-boot:run "-Dspring-boot.run.arguments=--app.seed=true"
   ```
   Después, cortarlo y levantarlo normal (sin el flag, o cada reinicio vuelve a borrar todo):
   ```bash
   ./mvnw spring-boot:run
   ```
4. La API queda en `http://localhost:4002`. Hibernate crea y actualiza las tablas al arrancar.

### Datos de ejemplo

El seed (`seed/DataSeeder.java` + `resources/seed/seed-data.json`) carga 7 categorías con 27 intereses,
43 experiencias en 9 ciudades con fotos (créditos en `resources/seed/CREDITOS-FOTOS.md`), fechas pasadas y futuras
(una agotada, con lista de espera), ~500 reservas, ~170 reseñas, 5 cupones (`BIENVENIDA15`, `PLAN20`, `FINDE10`;
`INVIERNO25` vencido y `PAUSADO30` desactivado), un regalo y 8 monedas.

| Cuenta | Contraseña | Para qué |
|---|---|---|
| `admin@plan.com` | `admin123` | Panel de administración |
| `sofia@plan.com` | `plan1234` | Cuenta de demo: reservas próximas y pasadas sin reseñar, guardados, un regalo recibido. No usó ningún cupón |
| `marcos@plan.com` (y el resto de los anfitriones de `seed-data.json`) | `plan1234` | Anfitriones |
| `tomas@plan.com` (y el resto de los compradores) | `plan1234` | Compradores |

Todas las cuentas del seed ya tienen el email verificado.

## Seguridad

- **Registro con verificación del email:** la cuenta queda sin verificar y llega un código de 6 dígitos al mail. Sin
  verificarlo no se puede iniciar sesión (el login responde 403 y manda un código nuevo).
- **Recuperar contraseña** con un código por mail. Los códigos se guardan hasheados (como la contraseña), vencen a
  los 15 minutos, se invalidan con 5 intentos fallidos y no se manda más de un mail por minuto a la misma cuenta.
- **JWT** (24 h) en el header `Authorization: Bearer …`. El rol no viaja en el token: se lee de la base en cada
  pedido, así que un cambio de rol o una cuenta desactivada se aplican enseguida.
- **Roles:** `CLIENTE` (toda cuenta compra y vende) y `ADMIN` (modera; no compra). Para crear otro admin: un admin le
  cambia el rol desde el panel (`PATCH /users/{id}/role`).
- **Estados HTTP:** 200/201/204 si salió bien (una lista vacía es `200 []`, nunca 404); 400 regla de negocio o dato
  inválido; 401 sin sesión o login incorrecto; 403 sin permiso o email sin verificar; 404 cuando se pide por id algo
  que no existe; 503 si no se pudo mandar un mail. Siempre con `{ status, message }` en castellano.

## Reglas de negocio

**Experiencias y fechas**
- Una experiencia tiene categoría, hasta 5 fotos, precio, descuento (0–90 %), hasta 8 intereses y fechas con cupo
  (el stock). En el catálogo público solo aparecen las publicadas, con alguna fecha futura y de anfitriones activos.
- El anfitrión puede pausarla (deja de verse) o darla de baja: se cancelan sus fechas futuras, se reembolsa a todos y
  les llega un aviso. Si alguna vez se vendió, queda guardada como `DELETED` (solo la ve el admin).
- A una fecha con reservas no se le puede cambiar el horario ni bajar el cupo por debajo de lo vendido. Cancelar una
  fecha reembolsa sus reservas y avisa a compradores, a quienes recibieron un regalo y a la lista de espera.

**Compra**
- Carrito → checkout: el back valida lugares (bloqueando la fila para que dos compras no vendan el mismo lugar),
  cupón, precio esperado y pago (simulado; la tarjeta `4000 0000 0000 0002` siempre se rechaza). Si algo falla, no
  se cobra nada. Nadie puede comprar su propia experiencia; un admin no compra.
- **Cupones:** un uso por persona (vuelve a estar disponible si esa orden se reembolsó entera). El descuento lo
  **absorbe PLAN**: el anfitrión cobra sobre el precio de lista.
- **Comisión de PLAN: 10 %**, incluida en el precio. Cada reserva informa lo pagado, lo que recibe el anfitrión y lo
  que le queda a PLAN.

**Cancelaciones**
- Quien tiene el voucher puede cancelar con reembolso total **hasta 48 h antes**; el anfitrión y el admin, hasta que
  empiece. La plata vuelve siempre a quien pagó. Cada reserva dice si quien la mira la puede cancelar y hasta cuándo
  (`refundable`, `refundableUntil`).

**Regalos**
- Se puede comprar para otra persona (nombre, email opcional y dedicatoria). Le llega un mail **sin el voucher**.
- El regalo pasa a ser suyo **solo cuando tiene una cuenta verificada con ese email** (en el momento, si ya la tiene;
  si no, cuando la crea y la verifica). Mientras tanto queda pendiente: el voucher es de quien pagó, que puede
  corregir el email o quedárselo. Así, un email mal escrito no le regala el plan a otra persona.
- Una vez entregado, el voucher es de quien lo recibió: lo ve, lo descarga, lo puede cancelar y reseña la
  experiencia. Quien pagó deja de ver el código.

**Reseñas, guardados, lista de espera y recomendaciones**
- Reseña quien fue (tiene el voucher, no se canceló y la experiencia ya terminó), una vez por experiencia. El
  anfitrión puede responder en público.
- En una fecha agotada, cualquiera se anota en la lista de espera; si se libera un lugar, el back les avisa a todos.
- **"Para vos":** el back puntúa el catálogo según los intereses de la persona, lo que guardó, lo que reservó y lo que
  vio hace poco, y devuelve cada recomendación con su motivo.
- **Monedas:** todo se cobra en pesos; las demás monedas son de referencia y el admin actualiza sus cotizaciones.

## Estructura

```
src/main/java/com/uade/tpo/demo/
  controllers/   un controller por recurso (auth, experiences, sessions, carts, orders, bookings, reviews, users,
                 coupons, categories, interests, currencies, favorites, notifications) y config/ (seguridad, JWT,
                 CORS, errores)
  service/       interfaces + impl/ con las reglas de negocio; MailService y VoucherImageService (voucher en PNG)
  repository/    Spring Data JPA (+ Specifications para la búsqueda del catálogo)
  entity/        el modelo de la base
  dtos/          request / response
  exceptions/    excepciones propias + GlobalExceptionHandler
  seed/          carga de datos de ejemplo (solo con --app.seed=true)
```
