import { Component, inject, signal, OnInit } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { forkJoin, finalize } from 'rxjs';
import { BankingService } from '../../core/banking.service';
import { SessionService } from '../../core/session.service';
import { apiError } from '../../lib/api-error';
import { Movement, UserProfile } from '../../lib/models';
import { Icon } from '../../shared/icon';
import { OperationPanel } from './operation-panel';

@Component({ selector: 'app-dashboard', imports: [CurrencyPipe, DatePipe, Icon, OperationPanel], templateUrl: './dashboard.page.html' })
export class DashboardPage implements OnInit {
  private bank = inject(BankingService);
  private session = inject(SessionService);
  readonly user = signal<UserProfile | null>(null);
  readonly movements = signal<Movement[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly hideBalance = signal(false);
  readonly showAll = signal(false);
  readonly filter = signal('all');
  readonly today = new Date();
  ngOnInit(): void { this.refresh(); }
  refresh(): void {
    if (this.loading()) return;
    const account = this.session.accountNumber();
    this.loading.set(true); this.error.set('');
    forkJoin({ user: this.bank.profile(account), history: this.bank.history(account) })
      .pipe(finalize(() => this.loading.set(false))).subscribe({
        next: result => { this.user.set(result.user); this.movements.set(result.history); },
        error: e => this.error.set(apiError(e))
      });
  }
  received(movement: Movement): boolean { return movement.destinationAccountNumber === this.session.accountNumber(); }
  label(movement: Movement): string {
    const labels = {deposit:'Depósito',withdraw:'Retiro',transfer:this.received(movement) ? 'Transferencia recibida' : 'Transferencia enviada',service_payment:'Pago de servicio'};
    return labels[movement.type];
  }
  filtered(): Movement[] {
    const list = this.movements().filter(m => this.filter() === 'all' || m.type === this.filter());
    return this.showAll() ? list : list.slice(0, 6);
  }
}
