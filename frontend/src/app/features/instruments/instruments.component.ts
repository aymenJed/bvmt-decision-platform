import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, Subject, switchMap } from 'rxjs';

import { InstrumentService } from '../../core/services/instrument.service';
import { Instrument, InstrumentType } from '../../core/models/instrument.model';

@Component({
  selector: 'app-instruments',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="card">
      <div class="toolbar">
        <h2>Instruments</h2>
        <input type="text" placeholder="Rechercher par ticker ou nom…"
               [(ngModel)]="query" (ngModelChange)="searchInput$.next($event)" />
        <select [(ngModel)]="typeFilter" (change)="applyType()">
          <option value="">Tous types</option>
          <option value="EQUITY">Actions</option>
          <option value="BOND">Obligations</option>
          <option value="SICAV">SICAV</option>
          <option value="FCP">FCP</option>
          <option value="INDEX">Indices</option>
        </select>
      </div>

      <table>
        <thead>
          <tr>
            <th>Ticker</th><th>Nom</th><th>Type</th><th>Secteur</th>
            <th>Marché</th><th>ISIN</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let i of instruments()" [routerLink]="['/instruments', i.id]">
            <td class="mono">{{ i.ticker }}</td>
            <td>{{ i.name }}</td>
            <td>
              <span class="type-badge">{{ i.instrumentType }}</span>
            </td>
            <td class="muted">{{ i.sector || '—' }}</td>
            <td class="muted">{{ i.market }}</td>
            <td class="mono muted">{{ i.isin }}</td>
          </tr>
          <tr *ngIf="instruments().length === 0">
            <td colspan="6" class="muted">Aucun instrument trouvé.</td>
          </tr>
        </tbody>
      </table>
    </div>
  `,
  styles: [`
    .toolbar { display: flex; gap: 12px; margin-bottom: 16px; align-items: center; }
    .toolbar h2 { margin: 0; }
    .toolbar input { flex: 1; max-width: 400px; }
    tbody tr { cursor: pointer; }
    .type-badge {
      font-size: 11px; padding: 2px 8px; border-radius: 10px;
      background: rgba(47,129,247,0.15); color: var(--accent);
      border: 1px solid var(--accent);
    }
  `]
})
export class InstrumentsComponent implements OnInit {
  private readonly api = inject(InstrumentService);

  readonly instruments = signal<Instrument[]>([]);
  readonly searchInput$ = new Subject<string>();

  query = '';
  typeFilter: '' | InstrumentType = '';

  ngOnInit(): void {
    this.loadAll();
    this.searchInput$
      .pipe(debounceTime(300), distinctUntilChanged(),
            switchMap(q => q.trim() ? this.api.search(q) : this.api.list(this.typeFilter || undefined)))
      .subscribe(list => this.instruments.set(list));
  }

  applyType(): void {
    this.api.list(this.typeFilter || undefined).subscribe(list => this.instruments.set(list));
  }

  private loadAll(): void {
    this.api.list().subscribe(list => this.instruments.set(list));
  }
}
