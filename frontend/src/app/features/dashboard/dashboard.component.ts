import { Component, OnDestroy, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';

import { WebSocketService } from '../../core/services/websocket.service';
import { SignalService } from '../../core/services/signal.service';
import {
  RsiAlert, SignalNotification, SignalStrength, SignalType, TradingSignal
} from '../../core/models/trading.model';

/**
 * Dashboard principal du trader.
 *
 * Décomposition fonctionnelle (SOA-friendly) :
 *   - [LEFT]   KPIs et récap marché  → synthèse
 *   - [CENTER] Flux temps réel signaux → priorité visuelle
 *   - [RIGHT]  Alertes RSI temps réel → action rapide
 *
 * Source de données :
 *   - chargement initial : REST /signals?days=7&limit=30
 *   - mises à jour       : WebSocket /topic/signals + /topic/alerts
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="dashboard-grid">
      <!-- Colonne stats/KPIs -->
      <aside class="card kpi-col">
        <h2>Récap marché</h2>
        <div class="kpi">
          <div class="kpi-label">Signaux (7j)</div>
          <div class="kpi-value">{{ allSignals().length }}</div>
        </div>
        <div class="kpi">
          <div class="kpi-label">Achats</div>
          <div class="kpi-value buy">{{ countByType('BUY') }}</div>
        </div>
        <div class="kpi">
          <div class="kpi-label">Ventes</div>
          <div class="kpi-value sell">{{ countByType('SELL') }}</div>
        </div>
        <div class="kpi">
          <div class="kpi-label">Alertes RSI actives</div>
          <div class="kpi-value warn">{{ alerts().length }}</div>
        </div>

        <h3>Top valeurs signalées</h3>
        <ul class="top-list">
          <li *ngFor="let t of topTickers()" [routerLink]="['/instruments', t.id]">
            <span class="ticker mono">{{ t.ticker }}</span>
            <span class="count muted">{{ t.count }} sig.</span>
          </li>
        </ul>
      </aside>

      <!-- Colonne principale : signaux -->
      <section class="card signals-col">
        <div class="section-head">
          <h2>Signaux de trading</h2>
          <span class="ws-status" [class.connected]="wsConnected()">
            {{ wsConnected() ? '● LIVE' : '○ hors ligne' }}
          </span>
        </div>

        <div class="signals-filters">
          <button class="btn" [class.active]="filter() === 'ALL'"  (click)="filter.set('ALL')">Tous</button>
          <button class="btn" [class.active]="filter() === 'BUY'"  (click)="filter.set('BUY')">Achat</button>
          <button class="btn" [class.active]="filter() === 'SELL'" (click)="filter.set('SELL')">Vente</button>
        </div>

        <table>
          <thead>
            <tr>
              <th>Ticker</th><th>Type</th><th>Force</th><th>Règle</th>
              <th>Prix</th><th>Confiance</th><th>Date</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let s of filteredSignals()" [routerLink]="['/instruments', s.instrumentId]">
              <td class="mono">{{ s.ticker }}</td>
              <td>
                <span class="badge"
                      [class.badge-buy]="s.type === 'BUY'"
                      [class.badge-sell]="s.type === 'SELL'"
                      [class.badge-hold]="s.type === 'HOLD'">
                  {{ s.type }}
                </span>
              </td>
              <td [class]="'strength-' + s.strength.toLowerCase()">{{ s.strength }}</td>
              <td class="muted">{{ s.ruleCode }}</td>
              <td class="mono">{{ s.referencePrice | number:'1.2-3' }}</td>
              <td>{{ s.confidence | number:'1.0-0' }}%</td>
              <td class="muted">{{ s.signalDate }}</td>
            </tr>
            <tr *ngIf="filteredSignals().length === 0">
              <td colspan="7" class="muted">Aucun signal pour ce filtre.</td>
            </tr>
          </tbody>
        </table>
      </section>

      <!-- Colonne alertes RSI -->
      <aside class="card alerts-col">
        <h2>Alertes RSI</h2>
        <p class="muted small">Survente / Surachat franchis — flux live</p>
        <div class="alert-item" *ngFor="let a of alerts()"
             [routerLink]="['/instruments', a.instrumentId]">
          <div class="alert-head">
            <span class="mono">{{ a.ticker }}</span>
            <span class="badge"
                  [class.badge-buy]="a.level === 'OVERSOLD'"
                  [class.badge-sell]="a.level === 'OVERBOUGHT'">
              {{ a.level }}
            </span>
          </div>
          <div class="alert-body">
            RSI = <strong>{{ a.rsiValue | number:'1.1-1' }}</strong>
            <span class="muted"> — @ {{ a.closePrice | number:'1.2-3' }} TND</span>
          </div>
          <div class="alert-date muted">{{ a.tradeDate }}</div>
        </div>
        <p class="muted" *ngIf="alerts().length === 0">Aucune alerte en cours.</p>
      </aside>
    </div>
  `,
  styles: [`
    .dashboard-grid {
      display: grid; grid-template-columns: 260px 1fr 320px; gap: 16px;
    }
    @media (max-width: 1100px) { .dashboard-grid { grid-template-columns: 1fr; } }
    h2 { font-size: 15px; margin: 0 0 12px; }
    h3 { font-size: 12px; text-transform: uppercase; color: var(--text-muted);
         margin: 20px 0 8px; }
    .kpi { display: flex; justify-content: space-between; align-items: baseline;
           padding: 6px 0; border-bottom: 1px dashed var(--border); }
    .kpi-label { font-size: 12px; color: var(--text-muted); }
    .kpi-value { font-size: 18px; font-weight: 700; font-family: var(--font-mono); }
    .kpi-value.buy  { color: var(--buy); }
    .kpi-value.sell { color: var(--sell); }
    .kpi-value.warn { color: var(--warn); }
    .top-list { list-style: none; padding: 0; margin: 0; }
    .top-list li {
      display: flex; justify-content: space-between; padding: 6px 8px;
      border-radius: var(--radius); cursor: pointer;
    }
    .top-list li:hover { background: rgba(255,255,255,0.04); }

    .section-head { display: flex; align-items: center; gap: 12px; }
    .ws-status {
      margin-left: auto; font-size: 11px; color: var(--text-muted);
    }
    .ws-status.connected { color: var(--buy); }
    .signals-filters { display: flex; gap: 6px; margin: 8px 0 14px; }
    .signals-filters .btn.active { border-color: var(--accent); color: var(--accent); }

    tbody tr { cursor: pointer; }

    .alert-item {
      padding: 10px; border: 1px solid var(--border); border-radius: var(--radius);
      margin-bottom: 10px; cursor: pointer;
    }
    .alert-item:hover { border-color: var(--accent); }
    .alert-head { display: flex; justify-content: space-between; margin-bottom: 6px; }
    .alert-date { font-size: 11px; margin-top: 4px; }
    .small { font-size: 11px; }
  `]
})
export class DashboardComponent implements OnInit, OnDestroy {

  private readonly ws         = inject(WebSocketService);
  private readonly signalApi  = inject(SignalService);
  private readonly destroy$   = new Subject<void>();

  readonly allSignals = signal<(TradingSignal | SignalNotification)[]>([]);
  readonly alerts     = signal<RsiAlert[]>([]);
  readonly filter     = signal<SignalType | 'ALL'>('ALL');
  readonly wsConnected = signal(true);

  readonly filteredSignals = computed(() => {
    const f = this.filter();
    const list = this.allSignals();
    return f === 'ALL' ? list : list.filter(s => (s as any).type === f);
  });

  readonly topTickers = computed(() => {
    const counts = new Map<string, { id: number; ticker: string; count: number }>();
    for (const s of this.allSignals()) {
      const key = s.ticker;
      const entry = counts.get(key) ?? { id: (s as any).instrumentId, ticker: key, count: 0 };
      entry.count++;
      counts.set(key, entry);
    }
    return Array.from(counts.values())
                .sort((a, b) => b.count - a.count)
                .slice(0, 5);
  });

  ngOnInit(): void {
    // 1) Charge les signaux récents via REST
    this.signalApi.recent(7, 30).subscribe(list => this.allSignals.set(list));

    // 2) Abonne au flux live et prepend chaque nouveau signal
    this.ws.signals$.pipe(takeUntil(this.destroy$)).subscribe(notif => {
      this.allSignals.update(list => [notif as any, ...list].slice(0, 100));
    });

    // 3) Flux alertes RSI
    this.ws.alerts$.pipe(takeUntil(this.destroy$)).subscribe(alert => {
      this.alerts.update(list => [alert, ...list].slice(0, 20));
    });
  }

  countByType(type: SignalType): number {
    return this.allSignals().filter(s => (s as any).type === type).length;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
