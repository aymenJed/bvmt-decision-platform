import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';

export const APP_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },

  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  {
    path: 'signals',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/signals/signals.component').then(m => m.SignalsComponent)
  },
  {
    path: 'instruments',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/instruments/instruments.component').then(m => m.InstrumentsComponent)
  },
  {
    path: 'instruments/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/instruments/instrument-detail.component').then(m => m.InstrumentDetailComponent)
  },
  {
    path: 'portfolio',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/portfolio/portfolio.component').then(m => m.PortfolioComponent)
  },

  { path: '**', redirectTo: 'dashboard' }
];
