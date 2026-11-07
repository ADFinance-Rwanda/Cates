import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/auth/auth.service';
import { canManageProjects } from '../../../core/auth/access';
import { Project } from '../models/project.model';
import { Task } from '../../tasks/models/task.model';
import { errorMessage } from '../../../core/services/api-error';

@Component({
  selector: 'app-project-detail',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <p><a class="back" routerLink="/projects">Back to projects</a></p>
    @if (loading()) { <p class="loading">Loading project…</p> }
    @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
    @if (project(); as current) {
      <header class="page-head">
        <div>
          <h1>{{ current.name }}</h1>
          <p>{{ current.description || 'No description' }}</p>
        </div>
        <span class="badge {{ current.status }}">{{ current.status }}</span>
      </header>
      @if (canEdit()) {
        <form class="panel form-card" [formGroup]="form" (ngSubmit)="save()">
          <h2>Edit project</h2>
          <div class="form-grid">
            <label>Name <input formControlName="name" /></label>
            <label>Status
              <select formControlName="status">
                <option value="ACTIVE">ACTIVE</option>
                <option value="ARCHIVED">ARCHIVED</option>
              </select>
            </label>
            <label class="wide">Description <input formControlName="description" /></label>
          </div>
          <div class="modal-actions"><button type="submit">Save</button></div>
        </form>
      }
      <section class="catalog" style="margin-top: 0.85rem">
        <div class="catalog-bar">
          <div>
            <h2>Tasks</h2>
            <p class="lede">Tasks in this project.</p>
          </div>
        </div>
        @if (tasks().length === 0) { <p class="empty">No tasks in this project.</p> }
        @if (tasks().length > 0) {
          <table>
            <thead><tr><th>Title</th><th>Status</th><th>Priority</th></tr></thead>
            <tbody>
              @for (task of tasks(); track task.id) {
                <tr>
                  <td><a [routerLink]="['/tasks', task.id]">{{ task.title }}</a></td>
                  <td><span class="badge {{ task.status }}">{{ task.status }}</span></td>
                  <td><span class="badge {{ task.priority }}">{{ task.priority }}</span></td>
                </tr>
              }
            </tbody>
          </table>
        }
      </section>
    }
  `
})
export class ProjectDetailPage {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly forms = inject(FormBuilder);
  protected readonly project = signal<Project | null>(null);
  protected readonly tasks = signal<Task[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly form = this.forms.nonNullable.group({
    name: ['', Validators.required],
    description: [''],
    status: ['ACTIVE', Validators.required]
  });

  constructor() {
    const id = this.route.snapshot.paramMap.get('id') ?? '';
    this.api.project(id).subscribe({
      next: (project) => {
        this.project.set(project);
        this.form.patchValue({
          name: project.name,
          description: project.description ?? '',
          status: project.status
        });
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      }
    });
    this.api.tasks({ project: id, size: 50 }).subscribe({
      next: (page) => this.tasks.set(page.items),
      error: () => this.tasks.set([])
    });
  }

  protected canEdit(): boolean {
    return canManageProjects(this.auth.me());
  }

  protected save(): void {
    const current = this.project();
    if (!current) return;
    const raw = this.form.getRawValue();
    this.api.updateProject(current.id, { name: raw.name, description: raw.description, status: raw.status }).subscribe({
      next: (project) => this.project.set(project),
      error: (err) => this.error.set(errorMessage(err))
    });
  }
}
