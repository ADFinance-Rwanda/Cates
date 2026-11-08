import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { firstValueFrom } from 'rxjs';
import { Me } from './access';
import { authConfig } from './auth.config';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly oauth = inject(OAuthService);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  readonly me = signal<Me | null>(null);
  readonly setupError = signal('');
  readonly selectedOrgId = signal<string | null>(sessionStorage.getItem('taskflow.org'));
  private initPromise: Promise<void> | null = null;

  init(): Promise<void> {
    this.initPromise ??= this.doInit();
    return this.initPromise;
  }

  isLoggedIn(): boolean {
    return this.oauth.hasValidAccessToken();
  }

  accessToken(): string | null {
    return this.oauth.getAccessToken();
  }

  async login(): Promise<void> {
    if (this.setupError()) {
      this.initPromise = null;
      this.setupError.set('');
    }
    await this.init();
    if (!this.setupError()) {
      this.oauth.initCodeFlow();
    }
  }

  logout(): void {
    this.me.set(null);
    sessionStorage.removeItem('taskflow.org');
    this.oauth.logOut();
  }

  selectOrganization(organizationId: string): void {
    sessionStorage.setItem('taskflow.org', organizationId);
    this.selectedOrgId.set(organizationId);
    void this.refreshMe().then(() => this.router.navigateByUrl('/dashboard'));
  }

  private async doInit(): Promise<void> {
    this.oauth.configure(authConfig());
    for (let attempt = 0; attempt < 30; attempt++) {
      try {
        await this.oauth.loadDiscoveryDocumentAndTryLogin();
        this.setupError.set('');
        if (this.oauth.hasValidAccessToken()) {
          await this.refreshMe();
        }
        return;
      } catch {
        await new Promise((resolve) => setTimeout(resolve, 2000));
      }
    }
    this.setupError.set('The sign-in service is not reachable. Wait a moment and try again.');
    this.initPromise = null;
  }

  async refreshMe(): Promise<void> {
    const me = await firstValueFrom(this.http.get<Me>('/api/me'));
    this.me.set(me);
  }
}
