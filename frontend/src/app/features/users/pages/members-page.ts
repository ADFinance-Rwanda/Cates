import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/auth/auth.service';
import { canManageMembers } from '../../../core/auth/access';
import { Member } from '../models/member.model';
import { errorMessage } from '../../../core/services/api-error';

@Component({
  selector: 'app-members',
  imports: [ReactiveFormsModule],
  template: `
    <section class="catalog">
      <div class="catalog-bar">
        <div>
          <h1>Members</h1>
          <p class="lede">People in this organization and the role they hold.</p>
        </div>
        @if (canEdit()) {
          <button type="button" (click)="adding.set(true)">Add member</button>
        }
      </div>
      @if (loading()) { <p class="loading">Loading members…</p> }
      @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
      @if (!loading() && members().length === 0) { <p class="empty">No members in this organization.</p> }
      @if (members().length > 0) {
        <table>
          <thead><tr><th>Name</th><th>Email</th><th>Role</th>@if (canEdit()) { <th>Change role</th> }</tr></thead>
          <tbody>
            @for (member of members(); track member.id) {
              <tr>
                <td>{{ member.displayName }}</td>
                <td>{{ member.email }}</td>
                <td><span class="badge ACTIVE">{{ member.role }}</span></td>
                @if (canEdit()) {
                  <td>
                    <select [value]="member.role" (change)="change(member, $event)">
                      <option>ORG_ADMIN</option>
                      <option>PROJECT_MANAGER</option>
                      <option>MEMBER</option>
                    </select>
                  </td>
                }
              </tr>
            }
          </tbody>
        </table>
      }
    </section>
    @if (adding()) {
      <div class="backdrop" (click)="adding.set(false)">
        <div class="modal" role="dialog" aria-modal="true" aria-label="Add member" (click)="$event.stopPropagation()">
          <h2>Add member</h2>
          <p>The person must already have signed in once so their account exists.</p>
          <form [formGroup]="form" (ngSubmit)="add()">
            <label>Email <input type="email" formControlName="email" /></label>
            <label>Role
              <select formControlName="role">
                <option>MEMBER</option>
                <option>PROJECT_MANAGER</option>
                <option>ORG_ADMIN</option>
              </select>
            </label>
            <div class="modal-actions">
              <button type="button" class="secondary" (click)="adding.set(false)">Cancel</button>
              <button type="submit" [disabled]="form.invalid">Add</button>
            </div>
          </form>
        </div>
      </div>
    }
  `
})
export class MembersPage {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly forms = inject(FormBuilder);
  protected readonly members = signal<Member[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly adding = signal(false);
  protected readonly form = this.forms.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    role: ['MEMBER', Validators.required]
  });

  constructor() {
    this.reload();
  }

  protected canEdit(): boolean {
    return canManageMembers(this.auth.me());
  }

  protected change(member: Member, event: Event): void {
    const role = (event.target as HTMLSelectElement).value;
    this.api.changeRole(member.id, role).subscribe({
      next: () => this.reload(),
      error: (err) => this.error.set(errorMessage(err))
    });
  }

  protected add(): void {
    if (this.form.invalid) return;
    const raw = this.form.getRawValue();
    this.api.addMember({ email: raw.email, role: raw.role }).subscribe({
      next: () => {
        this.form.reset({ email: '', role: 'MEMBER' });
        this.adding.set(false);
        this.reload();
      },
      error: (err) => this.error.set(errorMessage(err))
    });
  }

  private reload(): void {
    this.api.members().subscribe({
      next: (members) => {
        this.members.set(members);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      }
    });
  }
}
