# SmartBancs: API con cuentas de 8 digitos

Requiere el SQL actual del usuario: tipos `deposit`, `withdraw`, `transfer`, `service_payment`,
columna `description`, importes NUMERIC(15,2), fechas TIMESTAMPTZ y clave unica idempotency_key.
Java no ejecuta migraciones: CREATE TABLE IF NOT EXISTS no actualiza tablas anteriores.
No se modificaron SQL ni Docker Compose.

## Empezar

1. Levantar PostgreSQL con el esquema indicado y ejecutar Java 21 desde el IDE o Maven.
2. Recargar Maven para descargar Spring Security y las dependencias JWT agregadas al pom.
3. Importar `tests/SmartBancs.postman_collection.json` y ejecutar en orden.
   La coleccion registra personas nuevas, recibe sus cuentas e inicia sesion; no depende de los
   usuarios semilla DEMO_LOGIN_DISABLED, que no tienen contrasenas validas ni cuentas asignadas.

POST /users registra `{name,email,password}` y crea una cuenta con saldo 0 en la misma transaccion.
Responde con accountNumber, name, email, balance y createdAt. No se requiere conocer el UUID interno.
POST /auth/login recibe `{accountNumber,password}` y devuelve accessToken, tokenType y expiresIn.
Los demas endpoints requieren `Authorization: Bearer <accessToken>`.

## Usuarios y cuentas

- GET /users: lista el perfil del usuario autenticado (no es un listado administrativo de todos).
- GET /users/{accountNumber}: consulta el perfil y saldo de su cuenta.
- PUT /users/{accountNumber}: actualiza name y email de su propio usuario. La contrasena se cambia en /auth/change-password.
- DELETE /users/{accountNumber}: elimina usuario y cuenta solamente si es su unica cuenta, esta
  en cero y no tiene movimientos. El historial financiero no se borra.
- GET /accounts y GET /accounts/{accountNumber}: consulta su cuenta y saldo.
- GET /users/{accountNumber}/transactions: todos los movimientos de esa cuenta, enviados y recibidos.
- GET /transactions?accountNumber=12345678: el mismo historial. Sin filtro usa la cuenta del token.
- GET /transactions/{id} y GET /transactions/by-key/{key}: solo movimientos de su cuenta.

## Operaciones

Todos estos POST requieren un UUID en `Idempotency-Key`. Campos description opcionales, maximo 255.
Enviar amount como texto decimal, por ejemplo "25.50"; nunca calcular dinero con double.

| Ruta | Cuerpo |
| --- | --- |
| /transactions/deposits | accountNumber, amount, description |
| /transactions/withdrawals | accountNumber, amount, description |
| /transactions/transfers | destinationAccountNumber, amount, description |
| /transactions/service-payments | serviceCode, customerReference, amount, description |

La cuenta de origen en transferencias y pagos se obtiene del token. Depositos y retiros solo
se permiten para la cuenta autenticada. GET /transactions/services lista AGUA, LUZ e INTERNET.

Primera confirmacion: 201. Reintento con misma clave y datos: 200 y `Idempotency-Replayed: true`.
Otra operacion con esa clave: 409. Descripcion tambien forma parte de los datos comparados.
El frontend debe guardar la clave antes de enviar y conservarla despues de un timeout o reinicio.
Un error de saldo revierte tambien la reserva de la clave; una operacion rechazada puede reintentarse.

Se bloquean las cuentas en orden, se comprueba saldo y se guardan importe, movimiento y clave en una
misma transaccion. No se permiten saldos negativos, fracciones de centavo ni importes fuera de
NUMERIC(15,2). Un fallo previo al commit revierte todo; si se pierde la respuesta tras el commit,
el reintento devuelve la operacion existente. No hay endpoints para editar o borrar movimientos.

## Token y limites de la demo

Se usa Spring Security para verificar JWT HS256, firma, emisor y caducidad. El token dura 15 minutos
y identifica al usuario y la cuenta seleccionada. Cada acceso comprueba la propiedad actual de la
cuenta en la base de datos. Saber el numero de otra cuenta no permite retirar ni consultar su historial.
Las contrasenas usan PBKDF2 y no se devuelven. Idempotency-Key NO reemplaza al token de acceso.

Sin JWT_SECRET se genera una clave aleatoria al arrancar: al reiniciar Java hay que iniciar sesion
de nuevo. La idempotencia persiste en PostgreSQL y funciona tras obtener el nuevo token.
Para conservar sesiones entre reinicios o varias instancias, establecer la misma variable de entorno
JWT_SECRET con al menos 32 bytes aleatorios codificados en Base64. No subir el secreto al repositorio.
JWT no incluye refresh token. La API comprueba una huella de credenciales con la base de datos:
cambiar la contrasena o borrar el usuario invalida sus tokens anteriores. Esto agrega una consulta por
solicitud autenticada; esta demo prioriza simplicidad y revocacion sobre rendimiento maximo.

POST /auth/change-password recibe accountNumber, currentPassword, newPassword y confirmPassword.
Requiere JWT, comprueba que accountNumber pertenezca a la sesion (403 si es otra cuenta),
valida la contrasena anterior y la coincidencia de las nuevas. No es recuperacion por correo: quien
olvido realmente su contrasena no puede usar este flujo de demo.
GET /accounts/{number}/recipient requiere JWT y devuelve unicamente nombre y numero de cuenta
del destinatario; no revela email ni saldo. Angular usa este endpoint antes de transferir.

## Recomendaciones financieras

`GET /recommendations` requiere JWT y devuelve la ultima recomendacion del usuario.
`POST /recommendations/refresh` responde `202 Accepted` y solicita una generacion asincrona.
Spring calcula features agregadas de la cuenta autenticada y el servicio Python usa OpenAI
cuando existe `OPENAI_API_KEY` o `API_KEY`; si el proveedor falla se aplica `rules-v1`.
La recomendacion no puede modificar saldos ni ejecutar transacciones.
CORS permite localhost:4200 y 127.0.0.1:4200; no se aceptan cookies de autenticacion.

## Sincronizacion con Bancs

Cada transaccion confirmada tambien crea un evento en `bancs_outbox` dentro de la misma
transaccion de PostgreSQL. Un worker independiente reclama hasta 50 eventos, los entrega al
adaptador de Bancs y aplica reintentos sin bloquear la respuesta HTTP.

`GET /integration/bancs/status` devuelve los contadores de eventos `PENDING`, `PROCESSING`,
`SENT` y `FAILED`. En esta demo el adaptador es `MockBancsClient`; el contrato conserva
`transactionId` e `idempotencyKey` para sustituirlo por el core real sin duplicar operaciones.

Los depositos simulan ingresos de efectivo para la demo. No verifican que un cajero/proveedor haya
recibido dinero real. Los pagos acreditan una cuenta recaudadora local, sin contactar empresas reales.
Para uso real se necesitan validacion del ingreso externo, HTTPS, controles de acceso por roles,
limites de intentos, auditoria y revocacion de sesiones. No se acredita capacidad de 10000 TPS.
Las pruebas JUnit existentes del DTO credit/debit antiguo no cubren este contrato; no se modificaron.

Referencia JWT: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
