import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, tap, throwError } from 'rxjs';
import { API_URL } from '../lib/api.config';
import { BasicService, Movement, Operation, OperationBody, PendingOperation, Recipient, UserProfile } from '../lib/models';

@Injectable({ providedIn: 'root' })
export class BankingService {
  private http = inject(HttpClient);
  private readonly storageKey = 'smartbancs.pending-operation.';
  private memoryPending = new Map<string, PendingOperation>();

  profile(account: string) { return this.http.get<UserProfile>(`${API_URL}/users/${account}`); }
  history(account: string) { return this.http.get<Movement[]>(`${API_URL}/users/${account}/transactions`); }
  services() { return this.http.get<BasicService[]>(`${API_URL}/transactions/services`); }
  recipient(account: string) { return this.http.get<Recipient>(`${API_URL}/accounts/${account}/recipient`); }
  updateProfile(account: string, body: { name: string; email: string }) {
    return this.http.put<UserProfile>(`${API_URL}/users/${account}`, body);
  }
  pending(account: string): PendingOperation | null {
    let pending = this.memoryPending.get(account) ?? null;
    try {
      const stored = sessionStorage.getItem(this.storageKey + account);
      if (stored) pending = JSON.parse(stored) as PendingOperation;
    } catch { /* Si el navegador bloquea storage se conserva el reintento en memoria. */ }
    return pending?.account === account ? pending : null;
  }
  private savePending(account: string, value: PendingOperation | null): void {
    if (value) this.memoryPending.set(account, value);
    else this.memoryPending.delete(account);
    try {
      if (value) sessionStorage.setItem(this.storageKey + account, JSON.stringify(value));
      else sessionStorage.removeItem(this.storageKey + account);
    } catch { /* El JWT no se guarda aquí. Solo se recuerda una operación sin confirmar. */ }
  }
  operation(account: string, kind: Operation, body: OperationBody) {
    const routes: Record<Operation, string> = {
      deposit: 'deposits', withdraw: 'withdrawals', transfer: 'transfers', service_payment: 'service-payments'
    };
    let pending = this.pending(account);
    if (pending && (pending.kind !== kind || JSON.stringify(pending.body) !== JSON.stringify(body))) {
      return throwError(() => new Error('Primero confirma el estado de tu operación pendiente.'));
    }
    if (!pending) pending = { account, kind, body, key: crypto.randomUUID() };
    this.savePending(account, pending);
    return this.http.post<Movement>(`${API_URL}/transactions/${routes[kind]}`, body,
      { headers: { 'Idempotency-Key': pending.key } }).pipe(
      tap(() => this.savePending(account, null)),
      catchError((error: HttpErrorResponse) => {
        // Ante desconexión, 401 o error del servidor, conservar la clave para el reintento.
        if (error.status >= 400 && error.status < 500 && error.status !== 401 && error.status !== 408) {
          this.savePending(account, null);
        }
        return throwError(() => error);
      })
    );
  }
  checkPending(account: string) {
    const pending = this.pending(account);
    if (!pending) return throwError(() => new Error('No hay una operación pendiente.'));
    return this.http.get<Movement>(`${API_URL}/transactions/by-key/${pending.key}`)
      .pipe(tap(() => this.savePending(account, null)));
  }
}
