# Recomendaciones del MVP

Angular envia hasta 100 movimientos a POST /api/recommendations/refresh con JWT.
Java verifica sus IDs y titularidad en PostgreSQL, envia importes reales y direccion de los
movimientos a Python, guarda las recomendaciones y devuelve la lista en la misma respuesta (200).
No se envian cuentas, correos, claves, referencias de clientes ni tokens a OpenAI.

En .env configurar OPENAI_API_KEY (o API_KEY) y OPENAI_MODEL. La clave solo llega al contenedor Python.
Desde la raiz ejecutar: docker compose up -d --build recommendations backend nginx
Abrir http://localhost:8088 y pulsar Pedir recomendaciones. El boton permanece ocupado hasta terminar.
Para diagnosticar errores: docker compose logs --tail=50 recommendations backend
Sin movimientos devuelve orientacion general identificada como sin IA. Con movimientos, una clave
ausente/invalida o un fallo del proveedor produce un error visible, sin presentar un consejo fijo como IA.
Python espera como maximo 25 segundos al proveedor, Java 35 segundos a Python.
El listener automatico tras transacciones existente sigue usando el mismo generador en segundo plano.

Se conserva Chat Completions con JSON mode y se validan las recomendaciones antes de guardarlas:
https://developers.openai.com/api/docs/guides/structured-outputs
