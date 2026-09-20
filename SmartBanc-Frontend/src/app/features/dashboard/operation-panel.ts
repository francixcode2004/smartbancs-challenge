import { Component, Input, Output, EventEmitter, inject, signal, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { BankingService } from '../../core/banking.service';
import { BasicService, Movement, Operation, OperationBody, Recipient, UserProfile } from '../../lib/models';
import { apiError } from '../../lib/api-error';
import { Icon } from '../../shared/icon';

@Component({ selector: 'app-operation-panel', imports: [ReactiveFormsModule, CurrencyPipe, Icon], templateUrl: './operation-panel.html' })
export class OperationPanel implements OnInit {
  @Input({ required: true }) user!: UserProfile;
  @Output() completed = new EventEmitter<Movement>();
  private bank = inject(BankingService);
  private fb = inject(FormBuilder);
  readonly kind = signal<Operation>('transfer');
  readonly busy = signal(false);
  readonly searching = signal(false);
  readonly error = signal('');
  readonly info = signal('');
  readonly recipient = signal<Recipient | null>(null);
  readonly services = signal<BasicService[]>([]);
  readonly pending = signal(false);
  readonly result = signal<Movement | null>(null);
  readonly options: { kind: Operation; icon: string; label: string }[] = [
    {kind:'transfer',icon:'transfer',label:'Transferir'}, {kind:'deposit',icon:'deposit',label:'Depositar'},
    {kind:'withdraw',icon:'withdraw',label:'Retirar'}, {kind:'service_payment',icon:'receipt',label:'Servicios'}
  ];
  readonly form = this.fb.nonNullable.group({
    destination: [''], amount: ['', [Validators.required, Validators.pattern(/^[0-9]{1,13}([.,][0-9]{1,2})?$/)]],
    description: ['', Validators.maxLength(255)], serviceCode: ['AGUA'], customerReference: ['']
  });
  ngOnInit(): void {
    this.bank.services().subscribe({ next: value => this.services.set(value), error: () => this.services.set([]) });
    const pending = this.bank.pending(this.user.accountNumber);
    if (pending) {
      this.kind.set(pending.kind); this.pending.set(true);
      this.form.patchValue({ destination: pending.body.destinationAccountNumber ?? '', amount: pending.body.amount,
        description: pending.body.description ?? '', serviceCode: pending.body.serviceCode ?? 'AGUA', customerReference: pending.body.customerReference ?? '' });
      this.form.disable();
    }
  }
  choose(kind: Operation): void {
    if (this.busy() || this.pending()) return;
    this.kind.set(kind); this.error.set(''); this.info.set(''); this.result.set(null); this.recipient.set(null);
    this.form.reset({ destination:'', amount:'', description:'', serviceCode:'AGUA', customerReference:'' });
  }
  destinationChanged(): void { this.recipient.set(null); this.error.set(''); }
  lookup(): void {
    const number = this.form.controls.destination.value;
    if (!/^[0-9]{8}$/.test(number)) { this.error.set('Ingresa una cuenta de ocho dígitos.'); return; }
    if (number === this.user.accountNumber) { this.error.set('Elige una cuenta diferente a la tuya.'); return; }
    this.searching.set(true); this.error.set(''); this.recipient.set(null);
    this.bank.recipient(number).pipe(finalize(() => this.searching.set(false))).subscribe({
      next: recipient => { if (number === this.form.controls.destination.value) this.recipient.set(recipient); },
      error: e => this.error.set(apiError(e))
    });
  }
  submit(): void {
    if (this.busy()) return;
    const existing = this.bank.pending(this.user.accountNumber);
    const value = this.form.getRawValue();
    let body: OperationBody;
    if (existing) body = existing.body;
    else {
      this.form.markAllAsTouched();
      const amount = value.amount.trim().replace(',', '.');
      if (this.form.invalid || !/^[0-9]{1,13}(\.[0-9]{1,2})?$/.test(amount) || /^0+(\.0{1,2})?$/.test(amount)) {
        this.error.set('Ingresa un importe mayor a cero, con máximo dos decimales.'); return;
      }
      if (this.kind() === 'transfer' && (!this.recipient() || this.recipient()?.accountNumber !== value.destination)) {
        this.error.set('Consulta y confirma primero a tu destinatario.'); return;
      }
      if (this.kind() === 'service_payment' && (!value.customerReference.trim() || !this.services().some(s => s.code === value.serviceCode))) {
        this.error.set('Elige un servicio e ingresa la referencia del contrato.'); return;
      }
      body = { amount, description: value.description.trim() };
      if (this.kind() === 'deposit' || this.kind() === 'withdraw') body.accountNumber = this.user.accountNumber;
      if (this.kind() === 'transfer') body.destinationAccountNumber = value.destination;
      if (this.kind() === 'service_payment') { body.serviceCode = value.serviceCode; body.customerReference = value.customerReference.trim(); }
    }
    this.busy.set(true); this.form.disable(); this.error.set(''); this.info.set('');
    this.bank.operation(this.user.accountNumber, this.kind(), body).pipe(finalize(() => this.busy.set(false)))
      .subscribe({ next: value => this.success(value), error: e => {
        this.pending.set(!!this.bank.pending(this.user.accountNumber));
        if (!this.pending()) this.form.enable();
        this.error.set(apiError(e));
      }});
  }
  checkStatus(): void {
    if (this.busy()) return;
    this.busy.set(true); this.error.set(''); this.info.set('');
    this.bank.checkPending(this.user.accountNumber).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: result => this.success(result),
      error: e => {
        if (e instanceof HttpErrorResponse && e.status === 404) this.info.set('Todavía no hay una confirmación. Puedes reintentar de forma segura con los mismos datos.');
        else this.error.set(apiError(e));
      }
    });
  }
  private success(value: Movement): void {
    this.result.set(value); this.pending.set(false); this.recipient.set(null); this.form.enable();
    this.form.reset({ destination:'', amount:'', description:'', serviceCode:'AGUA', customerReference:'' });
    this.completed.emit(value);
  }
  label(): string { return this.options.find(o => o.kind === this.kind())?.label ?? 'Confirmar'; }
}
