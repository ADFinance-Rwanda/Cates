import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { canManageMembers } from '../../../core/auth/access';

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
        <a routerLink="/dashboard" routerLinkActive="active">Dashboard</a>
        <a routerLink="/projects" routerLinkActive="active">Projects</a>
        <a routerLink="/tasks" routerLinkActive="active">Tasks</a>
        @if (canManageMembers(auth.me())) {
          <a routerLink="/members" routerLinkActive="active">Members</a>
        }
      </nav>
    </aside>
  `
})
export class Sidebar {
  protected readonly auth = inject(AuthService);
  protected readonly canManageMembers = canManageMembers;
}
