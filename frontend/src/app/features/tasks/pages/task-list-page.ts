import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/auth/auth.service';
import { Page, canDeleteTasks } from '../../../core/auth/access';
import { Project } from '../../projects/models/project.model';
import { Task } from '../models/task.model';
import { Member } from '../../users/models/member.model';
import { errorMessage } from '../../../core/services/api-error';
import { ConfirmationDialog } from '../../../shared/components/confirmation-dialog/confirmation-dialog';
import { LoadingSpinner } from '../../../shared/components/loading-spinner/loading-spinner';

@Component({
  selector: 'app-tasks',
  imports: [ReactiveFormsModule, RouterLink, LoadingSpinner, ConfirmationDialog],
  template: `
    <section class="catalog">
      <div class="catalog-bar">
        <div>
          <h1>Tasks</h1>
          <p class="lede">Filter, open, and update work in this organization.</p>
        </div>
        <a class="button" routerLink="/tasks/new">New task</a>
      </div>
      <form class="catalog-bar" [formGroup]="filters" (ngSubmit)="search()">
        <div class="filters">
          <label>Search <input formControlName="q" /></label>
          <label>Status
            <select formControlName="status">
              <option value="">Any</option>
              <option>TODO</option>
              <option>IN_PROGRESS</option>
              <option>DONE</option>
            </select>
          </label>
          <label>Priority
            <select formControlName="priority">
              <option value="">Any</option>
              <option>LOW</option>
              <option>MEDIUM</option>
              <option>HIGH</option>
              <option>CRITICAL</option>
            </select>
          </label>
          <label>Project
            <select formControlName="project">
              <option value="">Any</option>
              @for (project of projects(); track project.id) {
                <option [value]="project.id">{{ project.name }}</option>
              }
            </select>
          </label>
          <label>Assignee
            <select formControlName="assignee">
              <option value="">Any</option>
              @for (member of members(); track member.userId) {
                <option [value]="member.userId">{{ member.displayName }}</option>
              }
            </select>
          </label>
          <label>Due from <input type="date" formControlName="dueFrom" /></label>
          <label>Due to <input type="date" formControlName="dueTo" /></label>
        </div>
        <button type="submit">Filter</button>
      </form>
    @if (loading()) { <app-loading-spinner message="Loading tasks…" /> }
    <app-confirmation-dialog
      [open]="pendingDelete() !== null"
      title="Delete task"
      [message]="pendingDelete() ? 'Delete “' + pendingDelete()!.title + '”?' : ''"
      confirmLabel="Delete"
      (confirm)="confirmDelete()"
      (cancel)="pendingDelete.set(null)"
    />
    @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
    @if (!loading() && page() && page()!.items.length === 0) {
      <p class="empty">No tasks match these filters.</p>
    }
    @if (page(); as current) {
      @if (current.items.length > 0) {
        <table>
          <thead>
            <tr><th>Title</th><th>Status</th><th>Priority</th><th>Due</th><th></th></tr>
          </thead>
          <tbody>
            @for (task of current.items; track task.id) {
              <tr>
                <td><a [routerLink]="['/tasks', task.id]">{{ task.title }}</a></td>
                <td><span class="badge {{ task.status }}">{{ task.status }}</span></td>
                <td><span class="badge {{ task.priority }}">{{ task.priority }}</span></td>
                <td>{{ task.dueDate || '—' }}</td>
                <td>
                  @if (canDelete()) {
                    <button type="button" class="danger" (click)="remove(task)">Delete</button>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
        <div class="pager">
          <button type="button" class="secondary" [disabled]="current.page === 0" (click)="go(current.page - 1)">Previous</button>
          <span>Page {{ current.page + 1 }} of {{ current.totalPages || 1 }}</span>
          <button type="button" class="secondary" [disabled]="current.page + 1 >= current.totalPages" (click)="go(current.page + 1)">Next</button>
        </div>
      }
    }
    </section>
  `
})
export class TasksPage {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly forms = inject(FormBuilder);
  protected readonly projects = signal<Project[]>([]);
  protected readonly members = signal<Member[]>([]);
  protected readonly page = signal<Page<Task> | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly pendingDelete = signal<Task | null>(null);
  protected readonly filters = this.forms.nonNullable.group({
    q: [''],
    status: [''],
    priority: [''],
    project: [''],
    assignee: [''],
    dueFrom: [''],
    dueTo: ['']
  });
  private pageIndex = 0;

  constructor() {
    this.api.projects(0, 100).subscribe({ next: (page) => this.projects.set(page.items), error: () => this.projects.set([]) });
    this.api.members().subscribe({ next: (members) => this.members.set(members), error: () => this.members.set([]) });
    this.load();
  }

  protected canDelete(): boolean {
    return canDeleteTasks(this.auth.me());
  }

  protected search(): void {
    this.pageIndex = 0;
    this.load();
  }

  protected go(page: number): void {
    this.pageIndex = page;
    this.load();
  }

  protected remove(task: Task): void {
    this.pendingDelete.set(task);
  }

  protected confirmDelete(): void {
    const task = this.pendingDelete();
    this.pendingDelete.set(null);
    if (!task) {
      return;
    }
    this.api.deleteTask(task.id).subscribe({
      next: () => this.load(),
      error: (err) => this.error.set(errorMessage(err))
    });
  }

  private load(): void {
    this.loading.set(true);
    const raw = this.filters.getRawValue();
    this.api.tasks({ ...raw, page: this.pageIndex, size: 20 }).subscribe({
      next: (page) => {
        this.page.set(page);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      }
    });
  }
}
