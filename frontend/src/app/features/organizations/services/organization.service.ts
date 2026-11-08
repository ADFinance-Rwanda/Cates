import { Injectable, inject, signal } from '@angular/core';
import { errorMessage } from '../../../core/services/api-error';
import { ApiService } from '../../../core/services/api.service';
import { Organization } from '../models/organization.model';

@Injectable({ providedIn: 'root' })
export class OrganizationService {
  private readonly api = inject(ApiService);
  readonly organizations = signal<Organization[]>([]);
  readonly loaded = signal(false);
  readonly loadError = signal('');

  load(): void {
    this.loadError.set('');
    this.api.organizations().subscribe({
      next: (organizations) => {
        this.organizations.set(organizations);
        this.loaded.set(true);
      },
      error: (err) => {
        this.organizations.set([]);
        this.loaded.set(true);
        this.loadError.set(errorMessage(err));
      }
    });
  }

  create(name: string) {
    return this.api.createOrganization(name);
  }

  update(id: string, body: { name: string; status: string }) {
    return this.api.updateOrganization(id, body);
  }
}
