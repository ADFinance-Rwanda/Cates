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
    await this.init();
    this.oauth.initCodeFlow();
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
    const config = authConfig();
    try {
      const remote = await firstValueFrom(this.http.get<{ issuer?: string; clientId?: string }>('/config.json'));
      if (remote.issuer) {
        config.issuer = remote.issuer;
      }
      if (remote.clientId) {
        config.clientId = remote.clientId;
      }
    } catch {
      // ng serve has no generated file. environment.ts stays the issuer.
    }
    this.oauth.configure(config);
    try {
      await this.oauth.loadDiscoveryDocumentAndTryLogin();
      if (this.oauth.hasValidAccessToken()) {
        await this.refreshMe();
      }
    } catch {
      this.setupError.set('The sign-in service is not reachable. Wait a moment and try again.');
    }
  }

  async refreshMe(): Promise<void> {
    const me = await firstValueFrom(this.http.get<Me>('/api/me'));
    this.me.set(me);
  }
}
