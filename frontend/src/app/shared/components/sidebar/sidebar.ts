import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { canManageMembers, canManageOrganizations, canReadAudit } from '../../../core/auth/access';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <aside class="sidebar">
      <div class="brand">
        <span class="mark" aria-hidden="true"></span>
        <span>
          <strong>Taskflow</strong>
          <em>Work platform</em>
        </span>
      </div>
      <nav aria-label="Primary">
        @if (canManageOrganizations(auth.me())) {
          <a routerLink="/organizations" routerLinkActive="active">Organizations</a>
        }
        <a routerLink="/dashboard" routerLinkActive="active">Dashboard</a>
        <a routerLink="/projects" routerLinkActive="active">Projects</a>
        <a routerLink="/tasks" routerLinkActive="active">Tasks</a>
        @if (canManageMembers(auth.me())) {
          <a routerLink="/members" routerLinkActive="active">Members</a>
        }
        @if (canReadAudit(auth.me())) {
          <a routerLink="/audit" routerLinkActive="active">Audit</a>
        }
      </nav>
    </aside>
  `
})
export class Sidebar {
  protected readonly auth = inject(AuthService);
  protected readonly canManageMembers = canManageMembers;
  protected readonly canReadAudit = canReadAudit;
  protected readonly canManageOrganizations = canManageOrganizations;
}
