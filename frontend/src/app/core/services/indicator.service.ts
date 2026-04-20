import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { IndicatorPoint } from '../models/trading.model';

@Injectable({ providedIn: 'root' })
export class IndicatorService {
  private readonly http = inject(HttpClient);
  private readonly url  = `${environment.apiBaseUrl}/indicators`;

  series(instrumentId: number, code: string, from: string, to?: string): Observable<IndicatorPoint[]> {
    const params: any = { code, from };
    if (to) params.to = to;
    return this.http.get<IndicatorPoint[]>(`${this.url}/instrument/${instrumentId}`, { params });
  }

  recompute(instrumentId: number, date: string): Observable<IndicatorPoint[]> {
    return this.http.post<IndicatorPoint[]>(
      `${this.url}/instrument/${instrumentId}/recompute`, null, { params: { date } });
  }
}
