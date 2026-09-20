import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-icon',
  template: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path [attr.d]="paths[name] || paths['wallet']"/></svg>`,
  styles: [`:host{display:inline-flex;width:22px;height:22px;flex-shrink:0}svg{width:100%;height:100%}`]
})
export class Icon {
  @Input() name = 'wallet';
  readonly paths: Record<string, string> = {
    home: 'M3 10 12 3l9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1z',
    wallet: 'M4 6h15v14H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h13v2M19 11h-6v5h6M16 13.5h.01',
    transfer: 'M3 7h17m-5-5 5 5-5 5M21 17H4m5-5-5 5 5 5',
    deposit: 'M12 3v12m-5-5 5 5 5-5M4 16v5h16v-5',
    withdraw: 'M12 16V3m-5 5 5-5 5 5M4 16v5h16v-5',
    receipt: 'M5 3h14v19l-3-2-4 2-4-2-3 2zM8 7h8M8 11h8M8 15h4',
    settings: 'M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8M9 3h6l1 3 3 1 2 5-2 5-3 1-1 3H9l-1-3-3-1-2-5 2-5 3-1z',
    logout: 'M9 4H4v16h5M10 12h11m-4-4 4 4-4 4',
    arrow: 'M5 12h14m-5-5 5 5-5 5',
    check: 'm5 12 4 4L19 6',
    lock: 'M6 10h12v11H6zM8 10V6a4 4 0 0 1 8 0v4M12 14v3',
    refresh: 'M20 8a8 8 0 0 0-14-3L3 8m0-5v5h5M4 16a8 8 0 0 0 14 3l3-3m0 5v-5h-5',
    user: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8M4 21v-2a8 8 0 0 1 16 0v2',
    eye: 'M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6',
    shield: 'm12 2 9 4v6c0 5-9 10-9 10S3 17 3 12V6zM8 12l3 3 5-6',
    mail: 'M3 5h18v14H3zM3 5l9 8 9-8',
    close: 'm6 6 12 12M6 18 18 6',
    chevron: 'm9 5 7 7-7 7',
    water: 'M12 2S5 10 5 15a7 7 0 0 0 14 0c0-5-7-13-7-13M8 15a4 4 0 0 0 4 4',
    bolt: 'm13 2-9 12h7l-1 8L21 9h-8z',
    globe: 'M2 12h20M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20M12 2c-6 5-6 15 0 20 6-5 6-15 0-20'
  };
}
