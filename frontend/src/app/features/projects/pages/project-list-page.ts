import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/auth/auth.service';
import { canManageProjects } from '../../../core/auth/access';
import { Project } from '../models/project.model';
import { errorMessage } from '../../../core/services/api-error';

@Component({
  selector: 'app-projects',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="catalog">
      <div class="catalog-bar">
        <div>
          <h1>Projects</h1>
          <p class="lede">Work grouped for this organization.</p>
        </div>
        @if (canCreate()) {
          <button type="button" (click)="creating.set(true)">Add project</button>
        }
      </div>
      @if (loading()) { <p class="loading">Loading projects…</p> }
      @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
      @if (!loading() && projects().length === 0) { <p class="empty">No projects yet.</p> }
      @if (projects().length > 0) {
        <table>
          <thead><tr><th>Name</th><th>Status</th><th>Updated</th></tr></thead>
          <tbody>
            @for (project of projects(); track project.id) {
              <tr>
                <td><a [routerLink]="['/projects', project.id]">{{ project.name }}</a></td>
                <td><span class="badge {{ project.status }}">{{ project.status }}</span></td>
                <td>{{ project.updatedAt.slice(0, 10) }}</td>
              </tr>
            }
          </tbody>
        </table>
      }
    </section>
    @if (creating()) {
      <div class="backdrop" (click)="creating.set(false)">
        <div class="modal" role="dialog" aria-modal="true" aria-label="Add project" (click)="$event.stopPropagation()">
          <h2>Add project</h2>
          <form [formGroup]="form" (ngSubmit)="create()">
            <label>Name <input formControlName="name" required /></label>
            <label>Description <input formControlName="description" /></label>
            <div class="modal-actions">
              <button type="button" class="secondary" (click)="creating.set(false)">Cancel</button>
              <button type="submit" [disabled]="form.invalid">Create</button>
            </div>
          </form>
        </div>
      </div>
    }
  `
})
export class ProjectsPage {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly forms = inject(FormBuilder);
  protected readonly projects = signal<Project[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly creating = signal(false);
  protected readonly form = this.forms.nonNullable.group({
    name: ['', Validators.required],
    description: ['']
  });

  constructor() {
    this.reload();
  }

  protected canCreate(): boolean {
    return canManageProjects(this.auth.me());
  }

  protected create(): void {
    if (this.form.invalid) return;
    this.api.createProject(this.form.getRawValue()).subscribe({
      next: () => {
        this.form.reset();
        this.creating.set(false);
        this.reload();
      },
      error: (err) => this.error.set(errorMessage(err))
    });
  }

  private reload(): void {
    this.loading.set(true);
    this.api.projects().subscribe({
      next: (page) => {
        this.projects.set(page.items);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      }
    });
  }
}
