#!/usr/bin/env bash
# ==============================================================
# eTrip - script para llenar la base con datos de prueba
# ==============================================================
# Que hace: registra usuarios, crea categorias, experiencias (con
# foto), sesiones, un cupon, un carrito, una compra y una resenia.
# Al final la base queda con datos reales en TODAS las tablas,
# lista para explorar con Insomnia o para la presentacion.
#
# Como correrlo (con el backend ya levantado, ./mvnw spring-boot:run):
#   Git Bash / Linux / Mac:  bash insomnia/seed.sh
#   Windows (curl.exe ya viene con Windows 10/11, pero el script
#   en si necesita un shell tipo bash -> usar Git Bash)
#
# Se puede correr mas de una vez: los usuarios/categorias/cupon
# repetidos van a fallar con 400 (ya existen) y el script sigue.
# ==============================================================

set -u
BASE_URL="${BASE_URL:-http://localhost:4002}"

# ---- helpers -------------------------------------------------

# body_of / status_of separan la respuesta de curl (le pegamos el
# status code al final con -w, separado por un salto de linea).
body_of()   { echo "$1" | sed '$d'; }
status_of() { echo "$1" | tail -n1; }

req_json() {
  # req_json METHOD PATH [BODY] [TOKEN]
  local method="$1" path="$2" body="${3:-}" token="${4:-}"
  local args=(-s -w '\n%{http_code}' -X "$method" "${BASE_URL}${path}" -H 'Content-Type: application/json')
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$body" ] && args+=(-d "$body")
  curl "${args[@]}"
}

json_num() { echo "$1" | grep -o "\"$2\":[0-9.]*" | head -1 | cut -d: -f2; }
json_str() { echo "$1" | grep -o "\"$2\":\"[^\"]*\"" | head -1 | sed -E "s/\"$2\":\"//;s/\"\$//"; }

# log/warn van a stderr: varias funciones de abajo se llaman como
# "X=$(funcion ...)" y si esto fuera a stdout se colaria en el valor.
log()  { echo ">> $*" >&2; }
warn() { echo "   (!) $*" >&2; }

# ---- 0. chequeo rapido de que el backend esta arriba ----------

if ! curl -s -o /dev/null --max-time 3 "${BASE_URL}/experiences"; then
  echo "No se pudo conectar a ${BASE_URL}. Levanta el backend primero (./mvnw spring-boot:run)." >&2
  exit 1
fi

# ---- 1. imagen de prueba (multipart pide al menos 1 foto) -----

IMG_FILE="$(mktemp --suffix=.png 2>/dev/null || echo /tmp/etrip_seed_img.png)"
# PNG 1x1 transparente, en base64
printf 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=' \
  | base64 -d > "$IMG_FILE" 2>/dev/null || \
  printf '\x89PNG\r\n\x1a\n' > "$IMG_FILE"  # fallback minimo si no hay `base64 -d`

# En Git Bash / MSYS, curl.exe es un binario nativo de Windows: dentro de un
# argumento compuesto como "-F images=@/tmp/xxx.png;type=..." NO le traduce
# solo la parte del path, y falla con "curl: (26) Failed to open/read local
# data". Si existe cygpath (Git Bash / Cygwin), lo pasamos a formato Windows.
IMG_PATH="$(cygpath -w "$IMG_FILE" 2>/dev/null || echo "$IMG_FILE")"

# ---- 2. usuarios ----------------------------------------------

register() {
  # register username firstname lastname email password
  local resp body status
  resp=$(req_json POST /api/v1/auth/register "{\"username\":\"$1\",\"firstname\":\"$2\",\"lastname\":\"$3\",\"email\":\"$4\",\"password\":\"$5\"}")
  status=$(status_of "$resp")
  if [ "$status" = "200" ]; then
    log "Usuario $4 creado."
  else
    warn "Usuario $4 no se creo (status $status, probablemente ya existe). Sigo."
  fi
}

login() {
  # login usernameOrEmail password  -> imprime el token por stdout
  local resp body
  resp=$(req_json POST /api/v1/auth/authenticate "{\"usernameOrEmail\":\"$1\",\"password\":\"$2\"}")
  body=$(body_of "$resp")
  json_str "$body" access_token
}

log "Registrando usuarios (ana=anfitriona, beto=comprador, admin=administracion)..."
register ana   Ana   Perez   ana@etrip.com   pass1234
register beto  Beto  Gomez   beto@etrip.com  pass1234
register admin Admin Etrip   admin@etrip.com pass1234

ANA_TOKEN=$(login ana@etrip.com pass1234)
BETO_TOKEN=$(login beto@etrip.com pass1234)
ADMIN_TOKEN=$(login admin@etrip.com pass1234)

if [ -z "$ANA_TOKEN" ] || [ -z "$BETO_TOKEN" ]; then
  echo "No se pudo loguear a ana/beto. Revisa la respuesta de /api/v1/auth/authenticate a mano." >&2
  exit 1
fi

# admin@etrip.com se registra como CLIENTE (el registro siempre fuerza CLIENTE).
# Para que los pasos de administracion (crear cupon, listar usuarios) funcionen
# hay que promoverlo a mano UNA vez:
#   UPDATE user SET role='ADMIN' WHERE email='admin@etrip.com';
# Si ya lo promoviste antes, este script lo detecta solo (ADMIN_IS_ADMIN abajo).
ADMIN_IS_ADMIN=false
if [ -n "$ADMIN_TOKEN" ]; then
  resp=$(req_json GET /users "" "$ADMIN_TOKEN")
  [ "$(status_of "$resp")" = "200" ] && ADMIN_IS_ADMIN=true
fi

# ---- 3. categorias (con ana) -----------------------------------

log "Creando categorias..."
create_category() {
  local resp body
  resp=$(req_json POST /experience-categories "{\"name\":\"$1\",\"description\":\"$2\"}" "$ANA_TOKEN")
  body=$(body_of "$resp")
  json_num "$body" id
}

CAT_TOURS=$(create_category "Tours urbanos" "Recorridos guiados por la ciudad")
CAT_COCINA=$(create_category "Clases de cocina" "Talleres gastronomicos")
[ -z "$CAT_TOURS" ]  && CAT_TOURS=1
[ -z "$CAT_COCINA" ] && CAT_COCINA=2
log "Categorias: tours=$CAT_TOURS cocina=$CAT_COCINA (si ya existian, se reusan ids 1/2 - ajustar si no coincide)"

# ---- 4. experiencias (multipart, con foto) ---------------------

log "Creando experiencias..."
create_experience() {
  # create_experience title description price location categoryId -> imprime id
  local resp status body
  resp=$(curl -s -w '\n%{http_code}' -X POST "${BASE_URL}/experiences" \
    -H "Authorization: Bearer $ANA_TOKEN" \
    -F "experience={\"title\":\"$1\",\"description\":\"$2\",\"price\":$3,\"location\":\"$4\",\"categoryId\":$5};type=application/json" \
    -F "images=@${IMG_PATH};type=image/png")
  status=$(status_of "$resp")
  body=$(body_of "$resp")
  if [ "$status" != "200" ] && [ "$status" != "201" ]; then
    warn "No se pudo crear la experiencia '$1' (status $status): $body"
    return
  fi
  json_num "$body" id
}

EXP1=$(create_experience "City tour Buenos Aires" "Recorrido por el centro historico" 25000 "Buenos Aires" "$CAT_TOURS")
EXP2=$(create_experience "Caminata por Palermo" "Paseo por Palermo Soho y Hollywood" 12000 "Palermo" "$CAT_TOURS")
EXP3=$(create_experience "Pastas caseras" "Amasado y salsa desde cero" 18000 "Recoleta" "$CAT_COCINA")
log "Experiencias creadas: $EXP1 $EXP2 $EXP3"

# Descuento de producto en una de ellas (para mostrar finalPrice)
if [ -n "$EXP1" ]; then
  req_json PATCH "/experiences/${EXP1}/discount" '{"discountPercentage": 20}' "$ANA_TOKEN" >/dev/null
  log "20% de descuento aplicado a la experiencia $EXP1."
fi

# ---- 5. sesiones (2 por experiencia, fechas a futuro) ----------

log "Creando sesiones..."
create_session() {
  local resp body
  resp=$(req_json POST /experience-sessions "{\"experienceId\":$1,\"startsAt\":\"$2\",\"endsAt\":\"$3\",\"capacity\":$4}" "$ANA_TOKEN")
  body=$(body_of "$resp")
  json_num "$body" id
}

SESSIONS=()
for exp in "$EXP1" "$EXP2" "$EXP3"; do
  [ -z "$exp" ] && continue
  s1=$(create_session "$exp" "2026-10-10T10:00:00" "2026-10-10T13:00:00" 10)
  s2=$(create_session "$exp" "2026-11-15T15:00:00" "2026-11-15T18:00:00" 8)
  [ -n "$s1" ] && SESSIONS+=("$s1")
  [ -n "$s2" ] && SESSIONS+=("$s2")
done
log "Sesiones creadas: ${SESSIONS[*]:-ninguna}"

# ---- 6. cupon (necesita admin) ----------------------------------

COUPON_CODE="DEMO2026"
if [ "$ADMIN_IS_ADMIN" = true ]; then
  req_json POST /discount-coupons "{\"code\":\"${COUPON_CODE}\",\"percentage\":15,\"validFrom\":\"2026-01-01T00:00:00\",\"validUntil\":\"2026-12-31T23:59:59\"}" "$ADMIN_TOKEN" >/dev/null
  log "Cupon $COUPON_CODE creado (15%)."
else
  warn "admin@etrip.com todavia es CLIENTE: no se pudo crear el cupon $COUPON_CODE."
  warn "Corre esto una vez en MySQL y volve a correr el script:"
  warn "  UPDATE user SET role='ADMIN' WHERE email='admin@etrip.com';"
fi

# ---- 7. carrito + checkout + resenia (con beto) -----------------

BETO_ID=$(json_num "$(body_of "$(req_json GET /users/me "" "$BETO_TOKEN")")" id)

if [ -n "${SESSIONS[0]:-}" ] && [ -n "$BETO_ID" ]; then
  log "Beto agrega al carrito la sesion ${SESSIONS[0]}..."
  req_json POST /carts/items "{\"userId\":${BETO_ID},\"experienceSessionId\":${SESSIONS[0]},\"quantity\":1}" "$BETO_TOKEN" >/dev/null

  log "Beto confirma la compra (checkout)..."
  ORDER_BODY='{}'
  [ "$ADMIN_IS_ADMIN" = true ] && ORDER_BODY="{\"couponCode\":\"${COUPON_CODE}\"}"
  resp=$(req_json POST /orders "$ORDER_BODY" "$BETO_TOKEN")
  if [ "$(status_of "$resp")" = "200" ] || [ "$(status_of "$resp")" = "201" ]; then
    log "Compra confirmada."
  else
    warn "El checkout devolvio status $(status_of "$resp"): $(body_of "$resp")"
  fi

  if [ -n "$EXP1" ]; then
    log "Beto deja una resenia en la experiencia $EXP1..."
    req_json POST /reviews "{\"experienceId\":${EXP1},\"rating\":5,\"comment\":\"Excelente, muy recomendable\"}" "$BETO_TOKEN" >/dev/null
  fi
else
  warn "No se pudo armar el carrito de beto (falta id de usuario o de sesion)."
fi

# ---- resumen -----------------------------------------------------

echo
echo "=== Listo ==="
echo "Usuarios:      ana@etrip.com / beto@etrip.com / admin@etrip.com  (password: pass1234)"
echo "Categorias:    $CAT_TOURS, $CAT_COCINA"
echo "Experiencias:  ${EXP1:-?}, ${EXP2:-?}, ${EXP3:-?}"
echo "Sesiones:      ${SESSIONS[*]:-ninguna}"
echo "Cupon:         ${COUPON_CODE} ($([ "$ADMIN_IS_ADMIN" = true ] && echo creado || echo 'NO creado, ver arriba'))"
echo
echo "Para usar Insomnia: logueate con cualquiera de estos usuarios y pega el"
echo "access_token en la variable 'token' del Environment (ver README/PRESENTACION.md)."
