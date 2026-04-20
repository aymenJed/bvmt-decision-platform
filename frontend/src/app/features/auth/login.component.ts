import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-wrapper">
      <div class="card login-card">
        <h1>BVMT Decision</h1>
        <p class="muted">Plateforme d'aide à la décision — marché tunisien</p>

        <label>Identifiant</label>
        <input type="text" [(ngModel)]="username" autocomplete="username" />

        <label>Mot de passe</label>
        <input type="password" [(ngModel)]="password" autocomplete="current-password"
               (keydown.enter)="submit()" />

        <div class="error" *ngIf="error()">{{ error() }}</div>

        <button class="btn btn-primary" (click)="submit()" [disabled]="loading()">
          {{ loading() ? 'Connexion…' : 'Se connecter' }}
        </button>

        <p class="muted small">
          Comptes démo (à changer en prod) :
          <br><code>admin / admin</code> — <code>trader / trader</code>
        </p>
      </div>
    </div>
  `,
  styles: [`
    .login-wrapper {
      min-height: 100vh; display: grid; place-items: center; padding: 20px;
    }
    .login-card { width: 360px; max-width: 100%; }
    .login-card h1 { margin: 0 0 4px; font-size: 20px; }
    .login-card label { display: block; margin: 14px 0 6px; font-size: 12px;
                        color: var(--text-muted); text-transform: uppercase; }
    .login-card input { width: 100%; }
    .login-card .btn { width: 100%; margin-top: 18px; }
    .error {
      margin-top: 10px; padding: 8px 10px; border-radius: var(--radius);
      background: rgba(248,81,73,0.15); color: var(--sell); font-size: 13px;
    }
    .small { font-size: 11px; margin-top: 20px; }
  `]
})
export class LoginComponent {
  private readonly auth   = inject(AuthService);
  private readonly router = inject(Router);

  username = '';
  password = '';
  readonly loading = signal(false);
  readonly error   = signal<string | null>(null);

  submit(): void {
    if (!this.username || !this.password) {
      this.error.set('Identifiant et mot de passe requis');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.auth.login({ username: this.username, password: this.password }).subscribe({
      next: () => { this.loading.set(false); this.router.navigateByUrl('/dashboard'); },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.status === 401 ? 'Identifiants invalides' : 'Erreur de connexion');
      }
    });
  }
}
