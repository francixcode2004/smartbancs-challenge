import { Injectable, signal } from '@angular/core';
import { AccessToken } from '../lib/models';

@Injectable({ providedIn: 'root' })
export class SessionService {
  private token = signal<string | null>(null);
  private expiresAt = 0;
  readonly accountNumber = signal('');

  start(response: AccessToken): void {
    this.token.set(response.accessToken);
    this.expiresAt = Date.now() + response.expiresIn * 1000;
    this.accountNumber.set(response.accountNumber);
  }
  accessToken(): string | null {
    if (Date.now() >= this.expiresAt) { this.clear(); return null; }
    return this.token();
  }
  clear(): void {
    this.token.set(null);
    this.expiresAt = 0;
    this.accountNumber.set('');
  }
  // El JWT nunca se escribe en localStorage, sessionStorage, cookies ni URLs.
}
