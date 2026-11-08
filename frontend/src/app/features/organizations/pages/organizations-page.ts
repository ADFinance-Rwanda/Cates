import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { errorMessage } from '../../../core/services/api-error';
import { LoadingSpinner } from '../../../shared/components/loading-spinner/loading-spinner';
import { Organization } from '../models/organization.model';
import { OrganizationService } from '../services/organization.service';

@Component({
  selector: 'app-organizations',
  imports: [ReactiveFormsModule, LoadingSpinner],
  template: `
    <section class="catalog">
      <div class="catalog-bar">
        <div>
          <h1>Organizations</h1>
          <p class="lede">Companies on this platform. Only the platform owner can add or suspend one.</p>
        </div>
        @if (!organizations.loadError()) {
          <button type="button" (click)="adding.set(true)">Add organization</button>
        }
      </div>
      @if (!organizations.loaded()) { <app-loading-spinner message="Loading organizations…" /> }
      @if (organizations.loadError()) { <p class="error" role="alert">{{ organizations.loadError() }}</p> }
      @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
      @if (organizations.loaded() && !organizations.loadError() && organizations.organizations().length === 0) {
        <p class="empty">No organizations yet.</p>
      }
      @if (organizations.organizations().length > 0) {
        <table>
          <thead>
            <tr><th>Name</th><th>Status</th></tr>
          </thead>
          <tbody>
            @for (org of organizations.organizations(); track org.id) {
              <tr>
                <td>{{ org.name }}</td>
                <td>
                  <select [value]="org.status" (change)="changeStatus(org, $event)" [attr.aria-label]="'Status for ' + org.name">
                    <option value="ACTIVE">Active</option>
                    <option value="SUSPENDED">Suspended</option>
                  </select>
                </td>
              </tr>
            }
          </tbody>
        </table>
      }
    </section>
    @if (adding()) {
      <div class="backdrop" (click)="close()">
        <div class="modal" role="dialog" aria-modal="true" aria-labelledby="add-org-title" (click)="$event.stopPropagation()">
          <h2 id="add-org-title">Add organization</h2>
          <p>The name is shown in the header when this company is selected.</p>
          <form [formGroup]="form" (ngSubmit)="add()">
            <label>Name <input formControlName="name" maxlength="200" /></label>
            @if (formError()) { <p class="error" role="alert">{{ formError() }}</p> }
            <div class="modal-actions">
              <button type="button" class="secondary" (click)="close()">Cancel</button>
              <button type="submit" [disabled]="form.invalid || saving()">Add</button>
            </div>
          </form>
        </div>
      </div>
    }
  `
})
export class OrganizationsPage {
  private readonly forms = inject(FormBuilder);
  protected readonly organizations = inject(OrganizationService);
  protected readonly error = signal('');
  protected readonly formError = signal('');
  protected readonly adding = signal(false);
  protected readonly saving = signal(false);
  protected readonly form = this.forms.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(200)]]
  });

  constructor() {
    this.organizations.load();
  }

  protected close(): void {
    this.adding.set(false);
    this.formError.set('');
    this.form.reset({ name: '' });
  }

  protected add(): void {
    if (this.form.invalid || this.saving()) {
      return;
    }
    this.saving.set(true);
    this.formError.set('');
    this.organizations.create(this.form.getRawValue().name.trim()).subscribe({
      next: () => {
        this.saving.set(false);
        this.close();
        this.organizations.load();
      },
      error: (err) => {
        this.saving.set(false);
        this.formError.set(errorMessage(err));
      }
    });
  }

  protected changeStatus(org: Organization, event: Event): void {
    const status = (event.target as HTMLSelectElement).value;
    this.organizations.update(org.id, { name: org.name, status }).subscribe({
      next: () => this.organizations.load(),
      error: (err) => {
        this.error.set(errorMessage(err));
        (event.target as HTMLSelectElement).value = org.status;
      }
    });
  }
}
