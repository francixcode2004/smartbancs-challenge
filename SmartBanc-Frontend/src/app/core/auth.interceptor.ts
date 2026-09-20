import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { SessionService } from './session.service';
import { API_URL } from '../lib/api.config';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(SessionService);
  const router = inject(Router);
  // Nunca enviar el token a otros orígenes ni a un formulario público de autenticación.
  const isApi = request.url.startsWith(API_URL + '/');
  const publicRequest = request.url === API_URL + '/auth/login'
    || (request.method === 'POST' && request.url === API_URL + '/users');
  const token = isApi && !publicRequest ? session.accessToken() : null;
  const authenticated = token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;
  return next(authenticated).pipe(catchError((error: HttpErrorResponse) => {
    if (isApi && !publicRequest && error.status === 401) {
      session.clear();
      void router.navigate(['/login'], { queryParams: { expired: '1' } });
    }
    return throwError(() => error);
  }));
};
