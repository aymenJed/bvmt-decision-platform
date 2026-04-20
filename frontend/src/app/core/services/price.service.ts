import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PriceBar } from '../models/trading.model';

@Injectable({ providedIn: 'root' })
export class PriceService {
  private readonly http = inject(HttpClient);
  private readonly url  = `${environment.apiBaseUrl}/prices`;

  history(instrumentId: number, from: string, to?: string): Observable<PriceBar[]> {
    const params: any = { from };
    if (to) params.to = to;
    return this.http.get<PriceBar[]>(`${this.url}/instrument/${instrumentId}`, { params });
  }

  latest(instrumentId: number): Observable<PriceBar> {
    return this.http.get<PriceBar>(`${this.url}/instrument/${instrumentId}/latest`);
  }
}
