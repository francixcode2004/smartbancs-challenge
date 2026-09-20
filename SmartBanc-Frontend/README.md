# SmartBancs Frontend · Angular 21

SPA con componentes standalone, formularios reactivos, rutas por funcionalidad, servicios HTTP
y JWT exclusivamente en memoria. No se almacena el token en localStorage, sessionStorage ni cookies.
Al recargar la pagina hay que iniciar sesion de nuevo. El cierre de sesion elimina el token en memoria.

## Ejecutar

Con el backend Java en localhost:8080 y PostgreSQL con el SQL actual:

```sh
npm install
npm start
```

Abrir http://localhost:4200. Crear un usuario desde Registro: la API genera la cuenta de ocho digitos
y el saldo inicial cero. Guardar ese numero para iniciar sesion con cuenta y contrasena.
Para transferir, registrar un segundo usuario, guardar su cuenta y volver a iniciar sesion con el primero.

La direccion de la API se configura solamente en `src/app/lib/api.config.ts`.
El backend permite CORS para localhost:4200 y 127.0.0.1:4200. Si cambia el origen, actualizar esa lista.

## Estructura

- lib: URL de la API, tipos de datos y mensajes de error.
- core: sesion en memoria, interceptor Bearer, guard, autenticacion y operaciones HTTP.
- features/auth: login, registro y cambio de contrasena de demostracion.
- features/dashboard: saldo, movimientos y panel de deposito, retiro, transferencia y servicios.
- features/settings: actualizacion del correo.
- layout: navegacion, perfil y cierre de sesion.
- shared: iconos locales; sin dependencias de iconos ni fuentes externas.

La consulta de destinatario solo muestra nombre y cuenta. No revela saldo ni email.
El backend autoriza cada operacion: el guard del navegador no reemplaza esa validacion.
El flujo “Olvide mi contrasena” requiere la ANTERIOR y dos copias de la nueva, como se solicito
para la demo. Sin sesion, primero autentica esa cuenta con la clave anterior. Con sesion, el
campo de cuenta esta deshabilitado y se obtiene de la sesion, nunca de la URL. La API exige JWT
y valida la titularidad incluso si se manipula el formulario. No es recuperacion real por email. Al cambiarla se invalidan los JWT anteriores.

## Reintentos de operaciones

Cada operacion genera un Idempotency-Key. Si hay desconexion o respuesta incierta, el panel conserva
la operacion y permite consultar su estado o reenviarla con la misma clave. Solo estos datos de una
operacion pendiente (cuenta, importe, descripcion y clave) se guardan en sessionStorage, nunca JWT
ni contrasenas. Se eliminan al confirmar o ante un rechazo definitivo. No cerrar la pestana mientras
haya una operacion incierta: sessionStorage dura la sesion de esa pestana.

Los depositos y pagos son simulados. No se presupone una confirmacion antes de la respuesta de la API.
El importe se envia como texto decimal; toda la contabilidad se calcula en Java/PostgreSQL.
Sin JWT_SECRET configurado, reiniciar Java invalida tokens; vuelve a iniciar sesion para reintentar.

## Verificacion

`npm run build` verifica TypeScript y las plantillas Angular y genera `dist/SmartBanc-Frontend/browser`.
Se eligio una SPA: las pantallas privadas no necesitan renderizado en servidor. El scaffold SSR original
queda sin uso; no se introdujeron pruebas unitarias nuevas.
La coleccion Postman del directorio tests documenta los endpoints y los casos de seguridad del backend.
