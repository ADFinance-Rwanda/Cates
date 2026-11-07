import { Component, inject, signal } from '@angular/core';
import { AuthService } from '../../../core/auth/auth.service';
import { Organization } from '../models/organization.model';
import { OrganizationService } from '../services/organization.service';

@Component({
  selector: 'app-organization-switcher',
  template: `
    @if (auth.me()?.superAdmin) {
      <label>
        Organization
        <select [value]="auth.selectedOrgId() || ''" (change)="onOrg($event)">
          <option value="">Select</option>
          @for (org of organizations(); track org.id) {
            <option [value]="org.id">{{ org.name }}</option>
          }
        </select>
      </label>
    }
  `
})
export class OrganizationSwitcher {
  protected readonly auth = inject(AuthService);
  private readonly organizationsApi = inject(OrganizationService);
  protected readonly organizations = signal<Organization[]>([]);

  constructor() {
    if (this.auth.me()?.superAdmin) {
      this.organizationsApi.list().subscribe({
        next: (orgs) => this.organizations.set(orgs),
        error: () => this.organizations.set([])
      });
    }
  }

  protected onOrg(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    if (value) {
      this.auth.selectOrganization(value);
    }
  }
}
