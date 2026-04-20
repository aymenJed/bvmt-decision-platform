import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Instrument } from '../models/instrument.model';

@Injectable({ providedIn: 'root' })
export class InstrumentService {
  private readonly http = inject(HttpClient);
  private readonly url  = `${environment.apiBaseUrl}/instruments`;

  list(type?: string): Observable<Instrument[]> {
    const params = type ? new HttpParams().set('type', type) : undefined;
    return this.http.get<Instrument[]>(this.url, { params });
  }

  search(q: string): Observable<Instrument[]> {
    return this.http.get<Instrument[]>(`${this.url}/search`, { params: { q } });
  }

  getById(id: number): Observable<Instrument> {
    return this.http.get<Instrument>(`${this.url}/${id}`);
  }

  getByTicker(ticker: string): Observable<Instrument> {
    return this.http.get<Instrument>(`${this.url}/by-ticker/${ticker}`);
  }
}
