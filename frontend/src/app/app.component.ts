import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <ng-container *ngIf="auth.currentUser(); else loggedOut">
      <header class="app-header">
        <div class="brand">BVMT <span class="muted">Decision</span></div>
        <nav class="nav">
          <a routerLink="/dashboard" routerLinkActive="active">Dashboard</a>
          <a routerLink="/signals"   routerLinkActive="active">Signaux</a>
          <a routerLink="/instruments" routerLinkActive="active">Instruments</a>
          <a routerLink="/portfolio" routerLinkActive="active">Portefeuille</a>
        </nav>
        <div class="user">
          <span class="muted">{{ auth.currentUser() }}</span>
          <button class="btn" (click)="auth.logout()">Déconnexion</button>
        </div>
      </header>
      <main class="app-main">
        <router-outlet />
      </main>
    </ng-container>
    <ng-template #loggedOut>
      <router-outlet />
    </ng-template>
  `,
  styles: [`
    .app-header {
      display: flex; align-items: center; gap: 24px;
      padding: 10px 20px; background: var(--bg-elev);
      border-bottom: 1px solid var(--border);
    }
    .brand { font-size: 16px; font-weight: 700; letter-spacing: 0.5px; }
    .nav { display: flex; gap: 4px; flex: 1; }
    .nav a {
      padding: 8px 14px; border-radius: var(--radius);
      color: var(--text-muted); font-size: 13px;
    }
    .nav a:hover { color: var(--text); text-decoration: none; background: rgba(255,255,255,0.04); }
    .nav a.active { color: var(--text); background: rgba(47,129,247,0.15); }
    .user { display: flex; align-items: center; gap: 12px; }
    .app-main { padding: 20px; max-width: 1600px; margin: 0 auto; }
  `]
})
export class AppComponent {
  readonly auth = inject(AuthService);
}
