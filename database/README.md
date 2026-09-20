# Cuentas, transferencias y servicios (demo local)

Todos los importes representan USD. Se mantiene controlador -> servicio -> repositorio.
Una transferencia mueve saldo entre dos cuentas. Un pago mueve saldo del cliente a la cuenta
recaudadora de AGUA, LUZ o INTERNET y guarda la referencia del contrato/cliente del proveedor.
El pago es una simulacion local: no consulta facturas ni comunica pagos a empresas reales.

## Actualizar la base existente

Deten Java y ejecuta en PowerShell desde la raiz del proyecto:

```powershell
docker compose up -d --wait
Get-Content -Raw database/migrations/002_bank_accounts.sql | docker compose exec -T postgres psql -U smartbancs -d smartbancs -v ON_ERROR_STOP=1
# Opcional: Pepito con 1000.00 y Maria con 100.00 de saldo ficticio.
Get-Content -Raw database/demo/demo.sql | docker compose exec -T postgres psql -U smartbancs -d smartbancs -v ON_ERROR_STOP=1
```

Inicia Java despues de terminar la migracion. `docker compose up` solo ejecuta los archivos de
`database/init` al inicializar un volumen vacio. No hace falta borrar el volumen existente.
Para una instalacion nueva, `database/init/database.sql` ya contiene el esquema completo.
Los datos de demo son opcionales incluso en instalaciones nuevas.

La migracion conserva los movimientos antiguos credit/debit y sus claves, visibles en el historial.
No los convierte en saldos: antes no existian cuentas que permitieran atribuirlos con seguridad.
Los endpoints anteriores POST /transactions con userId/type ya no existen.
El DTO TransactionRequest anterior se conserva por compatibilidad con el archivo de validacion
existente; ningun endpoint lo utiliza. No se modificaron ni ejecutaron esos tests.

## Endpoints

| Metodo | Ruta | Funcion |
| --- | --- | --- |
| POST | /accounts | Crea una cuenta con numero generado y saldo cero: {"userId":"UUID"} |
| GET | /accounts?userId=UUID | Cuentas de un usuario; sin filtro lista cuentas de clientes |
| GET | /accounts/{numero} | Consulta el saldo actual |
| GET | /transactions/services | Catalogo de servicios y cuentas recaudadoras |
| POST | /transactions/transfers | Transferencia entre cuentas de clientes |
| POST | /transactions/service-payments | Pago simulado de un servicio |
| GET | /transactions?userId=UUID | Movimientos enviados y recibidos; sin filtro lista todos |
| GET | /transactions/{id} | Consulta un movimiento |
| GET | /transactions/by-key/{clave} | Recupera el resultado de una operacion tras un reintento |

Los dos POST de operaciones requieren el header `Idempotency-Key` con un UUID elegido por el
cliente. Usar una clave nueva para una operacion nueva. Una repeticion con la misma clave y datos
devuelve el movimiento original (200, `Idempotency-Replayed: true`); la primera confirmacion devuelve
201. Reutilizar la clave con otros datos devuelve 409. La clave no es una credencial de seguridad.

Transferencia de Pepito a Maria:

```json
{
  "sourceAccountNumber": "800000000001",
  "destinationAccountNumber": "800000000002",
  "amount": "25.50"
}
```

Pago de agua de Pepito (con otra clave):

```json
{
  "sourceAccountNumber": "800000000001",
  "serviceCode": "AGUA",
  "customerReference": "CONTRATO-12345",
  "amount": "18.25"
}
```

Se rechazan importes <= 0, fracciones de centavo, importes mayores de 99999999.99, cuentas iguales,
cuentas inexistentes y saldo insuficiente. No hay un endpoint que permita asignarse saldo libremente.
Los saldos iniciales de demo no representan depositos reales ni una conciliacion contable historica.

## Consistencia y limites de la demo

Se usa BigDecimal, columnas NUMERIC y una transaccion de PostgreSQL para guardar el movimiento,
la clave y ambos saldos. Las cuentas se bloquean siempre por numero ascendente. Si falla antes del
commit, PostgreSQL revierte todo; si la respuesta se pierde despues del commit, reenviar la misma
clave recupera el resultado sin volver a descontar dinero. En un 503 o fallo de red conservar esa
clave; no generar otra automaticamente. Un rechazo de negocio no reserva la clave porque revierte.

Referencia de bloqueo: https://www.postgresql.org/docs/17/explicit-locking.html
Referencia READ COMMITTED / ON CONFLICT: https://www.postgresql.org/docs/17/transaction-iso.html

Se confirma el movimiento durante la solicitud HTTP, sin cola ni proceso por lotes. Esto no acredita
10 000 TPS ni una latencia garantizada: requiere medicion. Una cuenta recaudadora muy utilizada
serializa los pagos que modifican su saldo; este diseno prioriza sencillez y consistencia para la demo.
La API actual no autentica al titular: antes de exponerla se debe agregar autenticacion/autorizacion
para comprobar que quien envia una operacion puede utilizar la cuenta de origen.
