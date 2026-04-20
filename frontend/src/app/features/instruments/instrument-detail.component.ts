import { Component, OnInit, OnDestroy, inject, signal, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { forkJoin, Subject, takeUntil } from 'rxjs';

import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);

import { InstrumentService } from '../../core/services/instrument.service';
import { PriceService } from '../../core/services/price.service';
import { IndicatorService } from '../../core/services/indicator.service';
import { SignalService } from '../../core/services/signal.service';
import { Instrument } from '../../core/models/instrument.model';
import { IndicatorPoint, PriceBar, TradingSignal } from '../../core/models/trading.model';

/**
 * Vue détaillée d'un instrument :
 *   - entête avec métadonnées
 *   - chart principal : cours + SMA20 + SMA50 en overlay
 *   - sous-chart RSI avec zones survente/surachat
 *   - tableau des signaux récents
 *
 * Utilise Chart.js directement via canvas (pas ng2-charts) pour contrôler
 * finement la superposition des séries.
 */
@Component({
  selector: 'app-instrument-detail',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div *ngIf="instrument() as inst">
      <div class="card header-card">
        <div class="title">
          <span class="ticker mono">{{ inst.ticker }}</span>
          <span class="name">{{ inst.name }}</span>
          <span class="badge type-badge">{{ inst.instrumentType }}</span>
        </div>
        <div class="meta muted">
          <span>ISIN : <span class="mono">{{ inst.isin }}</span></span>
          <span>Secteur : {{ inst.sector || '—' }}</span>
          <span>Marché : {{ inst.market }}</span>
          <span>Devise : {{ inst.currency }}</span>
        </div>
        <div class="last-price" *ngIf="latestClose() as lc">
          <div class="price-big mono">{{ lc.close | number:'1.2-3' }}</div>
          <div class="price-var" [class.up]="lc.variationPct > 0" [class.down]="lc.variationPct < 0">
            {{ lc.variationPct > 0 ? '+' : '' }}{{ lc.variationPct | number:'1.2-2' }}%
          </div>
        </div>
      </div>

      <div class="card">
        <h3>Cours & moyennes mobiles</h3>
        <canvas #priceChart height="300"></canvas>
      </div>

      <div class="card">
        <h3>RSI(14)</h3>
        <canvas #rsiChart height="150"></canvas>
      </div>

      <div class="card">
        <h3>Signaux récents</h3>
        <table>
          <thead>
            <tr>
              <th>Date</th><th>Type</th><th>Force</th><th>Règle</th>
              <th>Prix</th><th>Confiance</th><th>Justification</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let s of signals()">
              <td class="muted">{{ s.signalDate }}</td>
              <td>
                <span class="badge"
                      [class.badge-buy]="s.type === 'BUY'"
                      [class.badge-sell]="s.type === 'SELL'">{{ s.type }}</span>
              </td>
              <td [class]="'strength-' + s.strength.toLowerCase()">{{ s.strength }}</td>
              <td class="muted">{{ s.ruleCode }}</td>
              <td class="mono">{{ s.referencePrice | number:'1.2-3' }}</td>
              <td>{{ s.confidence | number:'1.0-0' }}%</td>
              <td class="muted">{{ s.rationale }}</td>
            </tr>
            <tr *ngIf="signals().length === 0">
              <td colspan="7" class="muted">Aucun signal récent.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <div *ngIf="!instrument()" class="card muted">Chargement…</div>
  `,
  styles: [`
    .header-card { display: flex; flex-wrap: wrap; align-items: center; gap: 18px; }
    .title { display: flex; align-items: center; gap: 12px; }
    .ticker { font-size: 22px; font-weight: 700; }
    .name { font-size: 15px; color: var(--text-muted); }
    .meta { display: flex; gap: 16px; font-size: 12px; flex: 1; flex-wrap: wrap; }
    .last-price { text-align: right; }
    .price-big { font-size: 26px; font-weight: 700; }
    .price-var { font-size: 13px; }
    .price-var.up { color: var(--buy); }
    .price-var.down { color: var(--sell); }
    .type-badge {
      background: rgba(47,129,247,0.15); color: var(--accent);
      border: 1px solid var(--accent);
    }
    h3 { font-size: 13px; text-transform: uppercase; color: var(--text-muted);
         margin: 0 0 12px; letter-spacing: 0.5px; }
    canvas { width: 100% !important; }
  `]
})
export class InstrumentDetailComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('priceChart') priceCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('rsiChart')   rsiCanvas!:   ElementRef<HTMLCanvasElement>;

  private readonly route        = inject(ActivatedRoute);
  private readonly instrumentApi = inject(InstrumentService);
  private readonly priceApi     = inject(PriceService);
  private readonly indicatorApi = inject(IndicatorService);
  private readonly signalApi    = inject(SignalService);
  private readonly destroy$     = new Subject<void>();

  readonly instrument  = signal<Instrument | null>(null);
  readonly latestClose = signal<PriceBar | null>(null);
  readonly signals     = signal<TradingSignal[]>([]);

  private priceChart?: Chart;
  private rsiChart?:   Chart;

  // cache données pour construire les charts après init du canvas
  private prices:  PriceBar[]      = [];
  private sma20:   IndicatorPoint[] = [];
  private sma50:   IndicatorPoint[] = [];
  private rsi14:   IndicatorPoint[] = [];

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) return;

    const from = new Date(Date.now() - 180 * 24 * 3600 * 1000).toISOString().slice(0, 10);

    forkJoin({
      inst:   this.instrumentApi.getById(id),
      prices: this.priceApi.history(id, from),
      sma20:  this.indicatorApi.series(id, 'SMA_20', from),
      sma50:  this.indicatorApi.series(id, 'SMA_50', from),
      rsi:    this.indicatorApi.series(id, 'RSI_14', from),
      sigs:   this.signalApi.recent(90, 100)
    }).pipe(takeUntil(this.destroy$)).subscribe(({ inst, prices, sma20, sma50, rsi, sigs }) => {
      this.instrument.set(inst);
      this.prices = prices;
      this.latestClose.set(prices.at(-1) ?? null);
      this.sma20 = sma20;
      this.sma50 = sma50;
      this.rsi14 = rsi;
      this.signals.set(sigs.filter(s => s.instrumentId === id).slice(0, 30));
      this.renderCharts();
    });
  }

  ngAfterViewInit(): void {
    this.renderCharts();   // ré-essaie si les données arrivent avant le view init
  }

  ngOnDestroy(): void {
    this.priceChart?.destroy();
    this.rsiChart?.destroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  private renderCharts(): void {
    if (!this.priceCanvas?.nativeElement || this.prices.length === 0) return;

    this.priceChart?.destroy();
    this.rsiChart?.destroy();

    const labels = this.prices.map(p => p.date);
    const closes = this.prices.map(p => p.close);

    // Aligne les indicateurs sur les dates de prix (null si manquant)
    const alignOn = (series: IndicatorPoint[]) => {
      const map = new Map(series.map(p => [p.date, p.value]));
      return labels.map(d => map.get(d) ?? null);
    };

    this.priceChart = new Chart(this.priceCanvas.nativeElement, {
      type: 'line',
      data: {
        labels,
        datasets: [
          { label: 'Clôture',  data: closes, borderColor: '#2f81f7',
            backgroundColor: 'rgba(47,129,247,0.1)', tension: 0.1, pointRadius: 0, borderWidth: 2 },
          { label: 'SMA 20',   data: alignOn(this.sma20), borderColor: '#f0b429',
            borderDash: [4, 4], pointRadius: 0, borderWidth: 1.5 },
          { label: 'SMA 50',   data: alignOn(this.sma50), borderColor: '#f85149',
            borderDash: [2, 4], pointRadius: 0, borderWidth: 1.5 }
        ]
      },
      options: chartOptions()
    });

    if (this.rsiCanvas?.nativeElement && this.rsi14.length) {
      this.rsiChart = new Chart(this.rsiCanvas.nativeElement, {
        type: 'line',
        data: {
          labels,
          datasets: [
            { label: 'RSI 14', data: alignOn(this.rsi14),
              borderColor: '#9ea7b0', pointRadius: 0, borderWidth: 1.5 },
            { label: 'Surachat (70)', data: labels.map(() => 70),
              borderColor: '#f85149', borderDash: [3,3], pointRadius: 0, borderWidth: 1 },
            { label: 'Survente (30)', data: labels.map(() => 30),
              borderColor: '#2ea043', borderDash: [3,3], pointRadius: 0, borderWidth: 1 }
          ]
        },
        options: { ...chartOptions(), scales: { ...chartOptions().scales,
                    y: { min: 0, max: 100, ticks: { color: '#7d8590' },
                         grid: { color: 'rgba(255,255,255,0.05)' } } } }
      });
    }
  }
}

function chartOptions(): any {
  return {
    responsive: true, maintainAspectRatio: false,
    interaction: { mode: 'index', intersect: false },
    plugins: {
      legend: { labels: { color: '#e6edf3', boxWidth: 12, font: { size: 11 } } },
      tooltip: { mode: 'index', intersect: false }
    },
    scales: {
      x: { ticks: { color: '#7d8590', maxTicksLimit: 12 },
           grid: { color: 'rgba(255,255,255,0.04)' } },
      y: { ticks: { color: '#7d8590' },
           grid: { color: 'rgba(255,255,255,0.05)' } }
    }
  };
}
