import { Component, effect, inject } from '@angular/core';
import { AuthService } from '../../../core/auth/auth.service';
import { OrganizationService } from '../services/organization.service';

@Component({
  selector: 'app-organization-switcher',
  template: `
    @if (auth.me()?.superAdmin) {
      <label>
        Organization
        <select [value]="auth.selectedOrgId() || ''" (change)="onOrg($event)">
          <option value="">Select</option>
          @for (org of organizations.organizations(); track org.id) {
            <option [value]="org.id">{{ org.name }}</option>
          }
        </select>
      </label>
    }
  `
})
export class OrganizationSwitcher {
  protected readonly auth = inject(AuthService);
  protected readonly organizations = inject(OrganizationService);

  constructor() {
    effect(() => {
      if (this.auth.me()?.superAdmin) {
        this.organizations.load();
      }
    });
  }

  protected onOrg(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    if (value) {
      this.auth.selectOrganization(value);
    }
  }
}
