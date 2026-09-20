import { Component, inject, signal, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { BankingService } from '../../core/banking.service';
import { SessionService } from '../../core/session.service';
import { UserProfile } from '../../lib/models';
import { apiError } from '../../lib/api-error';
import { Icon } from '../../shared/icon';

@Component({ selector: 'app-settings', imports: [ReactiveFormsModule, RouterLink, Icon], templateUrl: './settings.page.html' })
export class SettingsPage implements OnInit {
  private bank = inject(BankingService);
  readonly session = inject(SessionService);
  readonly user = signal<UserProfile | null>(null);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]]
  });
  ngOnInit(): void {
    this.bank.profile(this.session.accountNumber()).subscribe({
      next: user => { this.user.set(user); this.form.setValue({email:user.email}); },
      error: e => this.error.set(apiError(e))
    });
  }
  save(): void {
    this.form.markAllAsTouched();
    const user = this.user();
    if (!user || this.form.invalid || this.busy()) return;
    this.busy.set(true); this.error.set(''); this.success.set('');
    this.bank.updateProfile(user.accountNumber, {name:user.name, email:this.form.controls.email.value.trim()})
      .pipe(finalize(() => this.busy.set(false))).subscribe({
        next: updated => { this.user.set(updated); this.success.set('Tu correo se actualizó correctamente.'); },
        error: e => this.error.set(apiError(e))
      });
  }
}
