import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { DashboardPage } from './features/dashboard/pages/dashboard-page';
import { ProjectDetailPage } from './features/projects/pages/project-detail-page';
import { ProjectsPage } from './features/projects/pages/project-list-page';
import { TaskFormPage } from './features/tasks/pages/task-form-page';
import { TasksPage } from './features/tasks/pages/task-list-page';
import { MembersPage } from './features/users/pages/members-page';
import { AuditPage } from './features/audit/pages/audit-page';
import { OrganizationsPage } from './features/organizations/pages/organizations-page';
import { AuthLayout } from './layout/auth-layout/auth-layout';
import { MainLayout } from './layout/main-layout/main-layout';

export const routes: Routes = [
  { path: '', pathMatch: 'full', component: AuthLayout },
  {
    path: '',
    component: MainLayout,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardPage },
      { path: 'organizations', component: OrganizationsPage },
      { path: 'projects', component: ProjectsPage },
      { path: 'projects/:id', component: ProjectDetailPage },
      { path: 'tasks', component: TasksPage },
      { path: 'tasks/new', component: TaskFormPage },
      { path: 'tasks/:id', component: TaskFormPage },
      { path: 'members', component: MembersPage },
      { path: 'audit', component: AuditPage }
    ]
  },
  { path: '**', redirectTo: '' }
];
