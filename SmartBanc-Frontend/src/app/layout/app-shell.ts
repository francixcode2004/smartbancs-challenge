import { Component, inject, signal, OnInit } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { SessionService } from '../core/session.service';
import { BankingService } from '../core/banking.service';
import { Icon } from '../shared/icon';

@Component({ selector: 'app-shell', imports: [RouterOutlet, RouterLink, RouterLinkActive, Icon], templateUrl: './app-shell.html' })
export class AppShell implements OnInit {
  private session = inject(SessionService);
  private router = inject(Router);
  private bank = inject(BankingService);
  readonly name = signal('Mi cuenta');
  readonly account = this.session.accountNumber;
  ngOnInit(): void {
    this.bank.profile(this.account()).subscribe({ next: user => this.name.set(user.name), error: () => {} });
  }
  logout(): void { this.session.clear(); void this.router.navigate(['/login']); }
}
