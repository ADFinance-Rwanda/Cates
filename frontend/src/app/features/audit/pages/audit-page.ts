import { Component, inject, signal } from '@angular/core';
import { Page } from '../../../core/auth/access';
import { errorMessage } from '../../../core/services/api-error';
import { LoadingSpinner } from '../../../shared/components/loading-spinner/loading-spinner';
import { AuditEntry } from '../models/audit.model';
import { AuditService } from '../services/audit.service';

const actionLabels: Record<string, string> = {
  TASK_CREATED: 'Task created',
  TASK_UPDATED: 'Task updated',
  TASK_DELETED: 'Task deleted',
  PROJECT_CREATED: 'Project created',
  PROJECT_UPDATED: 'Project updated',
  USER_ROLE_CHANGED: 'Role changed',
  ORGANIZATION_CREATED: 'Organization created',
  ORGANIZATION_UPDATED: 'Organization updated'
};

const fieldLabels: Record<string, string> = {
  title: 'Title',
  name: 'Name',
  status: 'Status',
  priority: 'Priority',
  projectId: 'Project',
  assigneeId: 'Assignee',
  dueDate: 'Due date',
  email: 'Email',
  role: 'Role',
  from: 'From',
  to: 'To',
  userId: 'Person'
};

@Component({
  selector: 'app-audit',
  imports: [LoadingSpinner],
  template: `
    <section class="catalog">
      <div class="catalog-bar">
        <div>
          <h1>Audit</h1>
          <p class="lede">A record of creates, updates, deletes, and role changes in this organization. Rows cannot be edited or deleted.</p>
        </div>
      </div>
      @if (loading()) { <app-loading-spinner message="Loading audit…" /> }
      @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
      @if (!loading() && page() && page()!.items.length === 0) {
        <p class="empty">No activity has been recorded yet.</p>
      }
      @if (page(); as current) {
        @if (current.items.length > 0) {
          <table>
            <thead>
              <tr><th>When</th><th>What happened</th><th>Details</th></tr>
            </thead>
            <tbody>
              @for (entry of current.items; track entry.id) {
                <tr>
                  <td>{{ when(entry.createdAt) }}</td>
                  <td>{{ action(entry) }}</td>
                  <td>{{ detail(entry.metadata) }}</td>
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
export class AuditPage {
  private readonly audit = inject(AuditService);
  protected readonly page = signal<Page<AuditEntry> | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  private pageIndex = 0;

  constructor() {
    this.load();
  }

  protected go(page: number): void {
    this.pageIndex = page;
    this.load();
  }

  protected when(value: string): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }
    return date.toLocaleString();
  }

  protected action(entry: AuditEntry): string {
    return actionLabels[entry.action] ?? entry.action;
  }

  protected detail(metadata: Record<string, unknown> | null): string {
    return describe(metadata);
  }

  private load(): void {
    this.loading.set(true);
    this.audit.list(this.pageIndex, 20).subscribe({
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

function describe(metadata: Record<string, unknown> | null | undefined): string {
  if (!metadata) {
    return '—';
  }
  const before = asRecord(metadata['before']);
  const after = asRecord(metadata['after']);
  if (before && after) {
    const keys = [...new Set([...Object.keys(before), ...Object.keys(after)])];
    const changes = keys.filter((key) => String(before[key] ?? '') !== String(after[key] ?? ''));
    if (changes.length === 0) {
      return 'Saved with no field changes';
    }
    return changes.map((key) => `${field(key)}: ${show(before[key])} → ${show(after[key])}`).join('; ');
  }
  const parts = Object.entries(metadata)
    .filter(([, value]) => value === null || typeof value !== 'object')
    .map(([key, value]) => `${field(key)}: ${show(value)}`);
  return parts.length > 0 ? parts.join(', ') : '—';
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value === null || typeof value !== 'object' || Array.isArray(value)) {
    return null;
  }
  return value as Record<string, unknown>;
}

function field(key: string): string {
  return fieldLabels[key] ?? key;
}

function show(value: unknown): string {
  if (value === null || value === undefined || value === '') {
    return '—';
  }
  return String(value);
}
