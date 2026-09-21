# Pipeline ETL de transacciones

Este pipeline toma un lote CSV sin procesar y produce dos salidas:

- `output/transactions_clean.csv`: filas validas, normalizadas y listas para consulta.
- `output/customer_features.json`: agregados mensuales por cuenta para analitica posterior.

El script usa solo la biblioteca estandar de Python. Descarta filas sin identificador,
cuenta de ocho digitos, tipo reconocido, direccion valida, monto positivo o fecha valida.
Normaliza timestamps a UTC, tipos de transaccion, descripciones y montos a dos decimales.

## Ejecucion

Desde la raiz del repositorio:

```powershell
python etl/transform_transactions.py
```

Tambien acepta archivos propios:

```powershell
python etl/transform_transactions.py `
  --input etl/input/transactions_raw.csv `
  --clean-output etl/output/transactions_clean.csv `
  --features-output etl/output/customer_features.json
```

Las features contienen ingresos, gastos, ahorro, proporcion de gasto, cantidad de
movimientos y categorias por cuenta y mes. No incluyen contrasenas, tokens ni correos.
