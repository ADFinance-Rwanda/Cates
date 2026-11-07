import { Component, inject } from '@angular/core';
import { AuthService } from '../../../core/auth/auth.service';
import { OrganizationSwitcher } from '../../../features/organizations/components/organization-switcher';

@Component({
  selector: 'app-header',
  imports: [OrganizationSwitcher],
  template: `
    <header class="topbar">
      <app-organization-switcher />
      <div class="who">
        <strong>{{ auth.me()?.displayName }}</strong>
        <span>{{ auth.me()?.organizationName || 'No organization' }}</span>
      </div>
      <span class="role">{{ auth.me()?.role }}</span>
      <button type="button" class="secondary" (click)="auth.logout()">Log out</button>
    </header>
  `
})
export class Header {
  protected readonly auth = inject(AuthService);
}
