import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/auth/auth.service';
import { canDeleteTasks } from '../../../core/auth/access';
import { Project } from '../../projects/models/project.model';
import { Task } from '../models/task.model';
import { Member } from '../../users/models/member.model';
import { errorMessage } from '../../../core/services/api-error';
import { ConfirmationDialog } from '../../../shared/components/confirmation-dialog/confirmation-dialog';
import { LoadingSpinner } from '../../../shared/components/loading-spinner/loading-spinner';

@Component({
  selector: 'app-task-form',
  imports: [ReactiveFormsModule, RouterLink, LoadingSpinner, ConfirmationDialog],
  template: `
    <p><a class="back" routerLink="/tasks">Back to tasks</a></p>
    <header class="page-head"><h1>{{ editing() ? 'Edit task' : 'New task' }}</h1></header>
    @if (loading()) { <app-loading-spinner /> }
    <app-confirmation-dialog
      [open]="confirmingDelete()"
      title="Delete task"
      message="Delete this task?"
      confirmLabel="Delete"
      (confirm)="confirmDelete()"
      (cancel)="confirmingDelete.set(false)"
    />
    @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
    @if (!loading() && !missing()) {
      <form class="panel form-card" [formGroup]="form" (ngSubmit)="save()">
        <div class="form-grid">
          <label>Project
            <select formControlName="projectId" required>
              <option value="">Select</option>
              @for (project of projects(); track project.id) {
                <option [value]="project.id">{{ project.name }}</option>
              }
            </select>
          </label>
          <label>Title <input formControlName="title" required maxlength="300" /></label>
          <label>Status
            <select formControlName="status">
              <option>TODO</option>
              <option>IN_PROGRESS</option>
              <option>DONE</option>
            </select>
          </label>
          <label>Priority
            <select formControlName="priority">
              <option>LOW</option>
              <option>MEDIUM</option>
              <option>HIGH</option>
              <option>CRITICAL</option>
            </select>
          </label>
          <label>Assignee
            <select formControlName="assigneeId">
              <option value="">Unassigned</option>
              @for (member of members(); track member.userId) {
                <option [value]="member.userId">{{ member.displayName }}</option>
              }
            </select>
          </label>
          <label>Due <input type="date" formControlName="dueDate" /></label>
          <label class="wide">Description <textarea formControlName="description" rows="4" maxlength="5000"></textarea></label>
        </div>
        @if (editing()) { <p>Version {{ form.controls.version.value }}</p> }
        <div class="row">
          <button type="submit" [disabled]="form.invalid || saving()">{{ saving() ? 'Saving…' : 'Save' }}</button>
          @if (editing() && canDelete()) {
            <button type="button" class="danger" (click)="remove()">Delete</button>
          }
        </div>
      </form>
    }
  `
})
export class TaskFormPage {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly forms = inject(FormBuilder);
  private readonly idempotencyKey = crypto.randomUUID();
  protected readonly projects = signal<Project[]>([]);
  protected readonly members = signal<Member[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly error = signal('');
  protected readonly missing = signal(false);
  protected readonly editing = signal(false);
  protected readonly confirmingDelete = signal(false);
  private taskId: string | null = null;
  protected readonly form = this.forms.nonNullable.group({
    projectId: ['', Validators.required],
    title: ['', Validators.required],
    description: [''],
    status: ['TODO', Validators.required],
    priority: ['MEDIUM', Validators.required],
    assigneeId: [''],
    dueDate: [''],
    version: [0]
  });

  constructor() {
    this.api.projects(0, 100).subscribe({ next: (page) => this.projects.set(page.items) });
    this.api.members().subscribe({ next: (members) => this.members.set(members) });
    const id = this.route.snapshot.paramMap.get('id');
    if (!id || id === 'new') {
      this.loading.set(false);
      return;
    }
    this.taskId = id;
    this.editing.set(true);
    this.api.task(id).subscribe({
      next: (task) => {
        this.fill(task);
        this.lockMemberFields(task);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.missing.set(true);
        this.loading.set(false);
      }
    });
  }

  protected canDelete(): boolean {
    return canDeleteTasks(this.auth.me());
  }

  protected save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.error.set('');
    const raw = this.form.getRawValue();
    const body = {
      projectId: raw.projectId,
      title: raw.title,
      description: raw.description || null,
      status: raw.status,
      priority: raw.priority,
      assigneeId: raw.assigneeId || null,
      dueDate: raw.dueDate || null,
      version: raw.version
    };
    const request = this.taskId
      ? this.api.updateTask(this.taskId, body)
      : this.api.createTask(body, this.idempotencyKey);
    request.subscribe({
      next: (task) => {
        this.saving.set(false);
        void this.router.navigate(['/tasks', task.id]);
        this.taskId = task.id;
        this.editing.set(true);
        this.fill(task);
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(errorMessage(err));
      }
    });
  }

  protected remove(): void {
    if (!this.taskId) return;
    this.confirmingDelete.set(true);
  }

  protected confirmDelete(): void {
    this.confirmingDelete.set(false);
    if (!this.taskId) return;
    this.api.deleteTask(this.taskId).subscribe({
      next: () => void this.router.navigateByUrl('/tasks'),
      error: (err) => this.error.set(errorMessage(err))
    });
  }

  private fill(task: Task): void {
    this.form.patchValue({
      projectId: task.projectId,
      title: task.title,
      description: task.description ?? '',
      status: task.status,
      priority: task.priority,
      assigneeId: task.assigneeId ?? '',
      dueDate: task.dueDate ?? '',
      version: task.version
    });
  }

  private lockMemberFields(task: Task): void {
    if (canDeleteTasks(this.auth.me())) {
      return;
    }
    const me = this.auth.me();
    const allowed = me && (me.id === task.ownerId || me.id === task.assigneeId);
    if (!allowed) {
      this.form.disable();
      return;
    }
    this.form.controls.projectId.disable();
    this.form.controls.assigneeId.disable();
  }
}
