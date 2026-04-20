import { Injectable, inject, OnDestroy } from '@angular/core';
import { RxStomp, RxStompConfig } from '@stomp/rx-stomp';
import { Observable, Subject, map, takeUntil } from 'rxjs';
import SockJS from 'sockjs-client';

import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import { RsiAlert, SignalNotification } from '../models/trading.model';

/**
 * Service WebSocket STOMP.
 *
 * Responsabilités :
 *   - établir la connexion SockJS/STOMP avec auth JWT
 *   - exposer des Observables pour les topics métier (signals, alerts)
 *   - reconnexion automatique en cas de coupure (géré par RxStomp)
 *
 * Usage depuis un composant :
 *   this.ws.signals$.subscribe(s => this.addSignal(s));
 */
@Injectable({ providedIn: 'root' })
export class WebSocketService implements OnDestroy {

  private readonly auth = inject(AuthService);
  private readonly rxStomp = new RxStomp();
  private readonly destroy$ = new Subject<void>();

  /** Flux de signaux de trading en direct. */
  readonly signals$: Observable<SignalNotification>;
  /** Flux d'alertes RSI (survente/surachat). */
  readonly alerts$:  Observable<RsiAlert>;

  constructor() {
    this.rxStomp.configure(this.buildConfig());
    this.rxStomp.activate();

    this.signals$ = this.rxStomp.watch('/topic/signals').pipe(
      map(m => JSON.parse(m.body) as SignalNotification),
      takeUntil(this.destroy$)
    );
    this.alerts$ = this.rxStomp.watch('/topic/alerts').pipe(
      map(m => JSON.parse(m.body) as RsiAlert),
      takeUntil(this.destroy$)
    );
  }

  /** Observable prix d'un ticker spécifique (pour futurs flux intraday). */
  prices$(ticker: string): Observable<any> {
    return this.rxStomp.watch(`/topic/prices/${ticker}`).pipe(
      map(m => JSON.parse(m.body)),
      takeUntil(this.destroy$)
    );
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.rxStomp.deactivate();
  }

  private buildConfig(): RxStompConfig {
    return {
      webSocketFactory: () => new SockJS(environment.wsUrl),
      connectHeaders: this.auth.accessToken
        ? { Authorization: `Bearer ${this.auth.accessToken}` }
        : {},
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
      reconnectDelay: 5_000,
      debug: () => {}    // silencieux en prod
    };
  }
}
