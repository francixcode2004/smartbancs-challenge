import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { SessionService } from '../../core/session.service';
import { apiError } from '../../lib/api-error';
import { Icon } from '../../shared/icon';

@Component({ selector: 'app-auth-page', imports: [RouterLink, ReactiveFormsModule, Icon], templateUrl: './auth.page.html' })
export class AuthPage {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private session = inject(SessionService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  readonly mode = this.route.snapshot.data['mode'] as 'login' | 'register' | 'password';
  readonly busy = signal(false);
  readonly error = signal('');
  readonly visiblePassword = signal(false);
  readonly banner = signal(this.route.snapshot.queryParamMap.has('registered')
    ? 'Tu cuenta está lista. Inicia sesión con tu correo.'
    : this.route.snapshot.queryParamMap.has('changed') ? 'Contraseña actualizada. Inicia sesión de nuevo.'
    : this.route.snapshot.queryParamMap.has('expired') ? 'Tu sesión terminó. Inicia sesión para continuar.' : '');
  readonly loginForm = this.fb.nonNullable.group({
    email: [this.route.snapshot.queryParamMap.get('email') ?? '', [Validators.required, Validators.email, Validators.maxLength(100)]],
    password: ['', [Validators.required, Validators.maxLength(128)]]
  });
  readonly registerForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128)]],
    confirmPassword: ['', Validators.required]
  });
  readonly passwordAccountLocked = !!this.session.accessToken();
  readonly passwordForm = this.fb.nonNullable.group({
    email: [{ value: this.route.snapshot.queryParamMap.get('email') ?? '', disabled: this.passwordAccountLocked }, [Validators.required, Validators.email, Validators.maxLength(100)]],
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128)]],
    confirmPassword: ['', Validators.required]
  });
  login(): void {
    if (this.busy()) return;
    this.loginForm.markAllAsTouched();
    if (this.loginForm.invalid) { this.error.set('Ingresa un correo válido y tu contraseña.'); return; }
    this.busy.set(true); this.error.set('');
    this.auth.login(this.loginForm.getRawValue()).pipe(finalize(() => this.busy.set(false)))
      .subscribe({ next: () => void this.router.navigate(['/app']), error: e => this.error.set(apiError(e)) });
  }
  register(): void {
    if (this.busy()) return;
    this.registerForm.markAllAsTouched();
    const value = this.registerForm.getRawValue();
    if (this.registerForm.invalid || !value.name.trim()) { this.error.set('Completa tus datos. La contraseña debe tener al menos ocho caracteres.'); return; }
    if (value.password !== value.confirmPassword) { this.error.set('Las contraseñas no coinciden.'); return; }
    this.busy.set(true); this.error.set('');
    this.auth.register({ name: value.name.trim(), email: value.email.trim(), password: value.password })
      .pipe(finalize(() => this.busy.set(false))).subscribe({
        next: user => void this.router.navigate(['/login'], { queryParams: { email: user.email, registered: '1' } }),
        error: e => this.error.set(apiError(e))
      });
  }
  changePassword(): void {
    if (this.busy()) return;
    if (this.passwordAccountLocked && !this.session.accessToken()) {
      void this.router.navigate(['/login'], { queryParams: { expired: '1' } });
      return;
    }
    this.passwordForm.markAllAsTouched();
    const value = this.passwordForm.getRawValue();
    if (this.passwordForm.invalid) { this.error.set('Completa los campos. La nueva contraseña debe tener al menos ocho caracteres.'); return; }
    if (value.newPassword !== value.confirmPassword) { this.error.set('Las nuevas contraseñas no coinciden.'); return; }
    this.busy.set(true); this.error.set('');
    this.auth.changePassword(value).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: () => { this.session.clear(); void this.router.navigate(['/login'], { queryParams: { email: value.email, changed: '1' } }); },
      error: e => this.error.set(apiError(e))
    });
  }
}
