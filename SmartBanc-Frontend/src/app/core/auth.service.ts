import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { switchMap, tap } from 'rxjs';
import { API_URL } from '../lib/api.config';
import { AccessToken, UserProfile } from '../lib/models';
import { SessionService } from './session.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private session = inject(SessionService);

  login(body: { accountNumber: string; password: string }) {
    return this.http.post<AccessToken>(`${API_URL}/auth/login`, body).pipe(tap(result => this.session.start(result)));
  }
  register(body: { name: string; email: string; password: string }) {
    return this.http.post<UserProfile>(`${API_URL}/users`, body);
  }
  changePassword(body: { accountNumber: string; currentPassword: string; newPassword: string; confirmPassword: string }) {
    if (this.session.accessToken()) {
      return this.http.post<void>(`${API_URL}/auth/change-password`, body);
    }
    // La recuperacion de demo acredita primero la cuenta con su clave anterior.
    // Este token temporal tampoco se guarda en el navegador.
    return this.http.post<AccessToken>(`${API_URL}/auth/login`, {
      accountNumber: body.accountNumber, password: body.currentPassword
    }).pipe(switchMap(result => this.http.post<void>(`${API_URL}/auth/change-password`, body, {
      headers: { Authorization: `Bearer ${result.accessToken}` }
    })));
  }
}
