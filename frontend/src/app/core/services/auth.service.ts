import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthRequest, AuthResponse } from '../models/auth.model';

const TOKEN_KEY   = 'bvmt_access_token';
const REFRESH_KEY = 'bvmt_refresh_token';
const USER_KEY    = 'bvmt_username';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly http   = inject(HttpClient);
  private readonly router = inject(Router);

  /** Signal réactif exposant l'état de connexion. */
  readonly currentUser = signal<string | null>(localStorage.getItem(USER_KEY));

  login(req: AuthRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiBaseUrl}/auth/login`, req)
      .pipe(tap(res => this.saveSession(res)));
  }

  register(req: AuthRequest & { email: string; fullName?: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiBaseUrl}/auth/register`, req)
      .pipe(tap(res => this.saveSession(res)));
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
    this.currentUser.set(null);
    this.router.navigateByUrl('/login');
  }

  get accessToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  get isAuthenticated(): boolean {
    return !!this.accessToken;
  }

  private saveSession(res: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY,   res.accessToken);
    localStorage.setItem(REFRESH_KEY, res.refreshToken);
    localStorage.setItem(USER_KEY,    res.username);
    this.currentUser.set(res.username);
  }
}
