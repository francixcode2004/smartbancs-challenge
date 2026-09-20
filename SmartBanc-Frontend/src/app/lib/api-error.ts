import { HttpErrorResponse } from '@angular/common/http';

export function apiError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) return 'No podemos conectar con el servicio. Comprueba que la API esté encendida.';
    if (error.status === 401) return 'Cuenta o contraseña incorrectas, o tu sesión ha caducado.';
    if (error.status === 403) return 'No tienes permiso para realizar esta operación.';
    if (typeof error.error?.message === 'string') return error.error.message;
    if (error.status === 404) return 'No encontramos la cuenta o el movimiento solicitado.';
    if (error.status >= 500) return 'El servicio no pudo confirmar la operación. Puedes volver a intentarlo.';
  }
  return error instanceof Error ? error.message : 'No pudimos completar la solicitud. Inténtalo de nuevo.';
}
