import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { TradingSignal } from '../models/trading.model';

@Injectable({ providedIn: 'root' })
export class SignalService {
  private readonly http = inject(HttpClient);
  private readonly url  = `${environment.apiBaseUrl}/signals`;

  recent(days = 30, limit = 50): Observable<TradingSignal[]> {
    return this.http.get<TradingSignal[]>(this.url, {
      params: { days: String(days), limit: String(limit) }
    });
  }

  byInstrument(id: number, date: string): Observable<TradingSignal[]> {
    return this.http.get<TradingSignal[]>(`${this.url}/instrument/${id}`, { params: { date } });
  }

  manualScan(date: string): Observable<string> {
    return this.http.post(`${this.url}/scan`, null,
                          { params: { date }, responseType: 'text' });
  }
}
