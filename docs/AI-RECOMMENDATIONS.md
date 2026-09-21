# Recomendaciones financieras con IA

## Arquitectura

Angular nunca conoce la clave de OpenAI ni llama al proveedor externo. El flujo es:

```text
Angular -> Spring Boot -> recommendations-service -> OpenAI
                         |
                         +-> fallback local si OpenAI no esta disponible
```

Spring valida el JWT y obtiene solo el historial de la cuenta autenticada. Luego calcula
features agregadas del periodo mas reciente: ingresos, gastos, ahorro, proporcion de gastos,
cantidad de movimientos y categorias. No se envian a OpenAI contrasenas, tokens, correo,
nombre ni numero de cuenta.

## Procesamiento asíncrono

Al confirmar una transaccion, Spring publica `TransactionCommittedEvent` despues del commit.
El listener llama a `RecommendationService.refreshAsync` usando un executor acotado. La
respuesta de la transferencia no espera a OpenAI y un fallo del proveedor no revierte la
operacion bancaria.

La API protegida expone:

```http
GET /recommendations
POST /recommendations/refresh
```

El primer endpoint devuelve la ultima recomendacion almacenada. El segundo solicita una
actualizacion y responde `202 Accepted`.

## Servicio Python

`services/recommendations` expone:

```http
POST /recommendations
```

Si existe `OPENAI_API_KEY` o `API_KEY`, usa el modelo indicado por `OPENAI_MODEL`. El prompt exige JSON,
limita la respuesta a recomendaciones educativas y prohíbe promesas de rendimiento,
inversiones especificas y ejecucion de operaciones. Si no hay clave, hay timeout o la
respuesta es invalida, se usan reglas locales `rules-v1`.

Configurar localmente sin subir secretos:

```powershell
$env:OPENAI_API_KEY = 'sk-...'
docker compose up -d --build
```

El fallback permite demostrar el flujo completo sin consumir la API ni bloquear el MVP.

## Ciclo de vida

- El ETL genera features desde transacciones limpias.
- `prompt_version` y `model_name` se guardan junto a cada recomendacion.
- Se monitorean latencia, errores, uso de tokens y fuente (`openai` o `fallback`).
- Se revisa data drift comparando distribuciones mensuales de ingresos, gastos y categorias.
- Se actualizan el prompt o el modelo mediante una nueva version y una prueba offline.
- Las recomendaciones son educativas; nunca modifican saldos ni ejecutan transacciones.
