import { Component, inject, signal, OnInit } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { forkJoin, finalize } from 'rxjs';
import { BankingService } from '../../core/banking.service';
import { RecommendationService } from '../../core/recommendation.service';
import { SessionService } from '../../core/session.service';
import { apiError } from '../../lib/api-error';
import { Movement, Recommendation, UserProfile } from '../../lib/models';
import { Icon } from '../../shared/icon';
import { OperationPanel } from './operation-panel';

@Component({ selector: 'app-dashboard', imports: [CurrencyPipe, DatePipe, Icon, OperationPanel], templateUrl: './dashboard.page.html' })
export class DashboardPage implements OnInit {
  private bank = inject(BankingService);
  private recommendations = inject(RecommendationService);
  private session = inject(SessionService);
  readonly user = signal<UserProfile | null>(null);
  readonly movements = signal<Movement[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly hideBalance = signal(false);
  readonly showAll = signal(false);
  readonly filter = signal('all');
  readonly recommendation = signal<Recommendation | null>(null);
  readonly recommendationItems = signal<Recommendation[]>([]);
  readonly recommendationBusy = signal(false);
  readonly recommendationMessage = signal('');
  readonly today = new Date();
  ngOnInit(): void { this.refresh(); }
  refresh(): void {
    if (this.loading()) return;
    const account = this.session.accountNumber();
    this.loading.set(true); this.error.set('');
    forkJoin({ user: this.bank.profile(account), history: this.bank.history(account) })
      .pipe(finalize(() => this.loading.set(false))).subscribe({
        next: result => {
          this.user.set(result.user);
          this.movements.set(result.history);
          this.loadRecommendation();
        },
        error: e => this.error.set(apiError(e))
      });
  }
  loadRecommendation(): void {
    this.recommendations.latest().subscribe({
      next: response => {
        if (this.recommendationBusy()) return;
        const item = response.status === 200 ? response.body : null;
        this.recommendation.set(item);
        this.recommendationItems.set(item ? [item] : []);
      },
      error: () => this.recommendation.set(null)
    });
  }
  requestRecommendation(): void {
    if (this.recommendationBusy()) return;
    this.recommendationBusy.set(true);
    this.recommendationMessage.set('Analizando tus movimientos…');
    this.recommendations.refresh(this.movements()).pipe(finalize(() => this.recommendationBusy.set(false))).subscribe({
      next: items => {
        this.recommendationItems.set(items);
        this.recommendation.set(items[0] ?? null);
        this.recommendationMessage.set(items[0]?.source === 'openai'
          ? 'Recomendaciones generadas con IA.' : 'Orientación general: todavía no hay movimientos para analizar.');
      },
      error: error => this.recommendationMessage.set(apiError(error))
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
