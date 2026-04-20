import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { SignalService } from '../../core/services/signal.service';
import { TradingSignal } from '../../core/models/trading.model';

@Component({
  selector: 'app-signals',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="card">
      <div class="toolbar">
        <h2>Historique des signaux</h2>
        <label>Sur les derniers
          <select [(ngModel)]="days" (change)="reload()">
            <option [value]="1">1 jour</option>
            <option [value]="7">7 jours</option>
            <option [value]="30">30 jours</option>
            <option [value]="90">90 jours</option>
          </select>
        </label>
      </div>

      <table>
        <thead>
          <tr>
            <th>Date</th><th>Ticker</th><th>Nom</th><th>Type</th><th>Force</th>
            <th>Règle</th><th>Prix</th><th>Justification</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let s of signals()" [routerLink]="['/instruments', s.instrumentId]">
            <td class="muted">{{ s.signalDate }}</td>
            <td class="mono">{{ s.ticker }}</td>
            <td>{{ s.name }}</td>
            <td>
              <span class="badge"
                    [class.badge-buy]="s.type === 'BUY'"
                    [class.badge-sell]="s.type === 'SELL'"
                    [class.badge-hold]="s.type === 'HOLD'">{{ s.type }}</span>
            </td>
            <td [class]="'strength-' + s.strength.toLowerCase()">{{ s.strength }}</td>
            <td class="muted">{{ s.ruleCode }}</td>
            <td class="mono">{{ s.referencePrice | number:'1.2-3' }}</td>
            <td class="muted">{{ s.rationale }}</td>
          </tr>
          <tr *ngIf="signals().length === 0">
            <td colspan="8" class="muted">Aucun signal sur la période.</td>
          </tr>
        </tbody>
      </table>
    </div>
  `,
  styles: [`
    .toolbar {
      display: flex; align-items: center; gap: 16px; margin-bottom: 16px;
    }
    .toolbar h2 { margin: 0; flex: 1; }
    tbody tr { cursor: pointer; }
  `]
})
export class SignalsComponent implements OnInit {
  private readonly api = inject(SignalService);

  readonly signals = signal<TradingSignal[]>([]);
  days = 7;

  ngOnInit(): void { this.reload(); }

  reload(): void {
    this.api.recent(this.days, 200).subscribe(list => this.signals.set(list));
  }
}
