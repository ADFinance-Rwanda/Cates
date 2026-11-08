import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Page } from '../auth/access';
import { Dashboard } from '../../features/dashboard/models/dashboard.model';
import { Organization } from '../../features/organizations/models/organization.model';
import { Project } from '../../features/projects/models/project.model';
import { Task } from '../../features/tasks/models/task.model';
import { Member } from '../../features/users/models/member.model';
import { AuditEntry } from '../../features/audit/models/audit.model';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  me() {
    return this.http.get('/api/me');
  }

  organizations(): Observable<Organization[]> {
    return this.http.get<Organization[]>('/api/organizations');
  }

  createOrganization(name: string): Observable<Organization> {
    return this.http.post<Organization>('/api/organizations', { name });
  }

  updateOrganization(id: string, body: { name: string; status: string }): Observable<Organization> {
    return this.http.put<Organization>(`/api/organizations/${id}`, body);
  }

  projects(page = 0, size = 20): Observable<Page<Project>> {
    return this.http.get<Page<Project>>('/api/projects', { params: { page, size } });
  }

  project(id: string): Observable<Project> {
    return this.http.get<Project>(`/api/projects/${id}`);
  }

  createProject(body: { name: string; description?: string }): Observable<Project> {
    return this.http.post<Project>('/api/projects', body);
  }

  updateProject(id: string, body: { name: string; description?: string; status: string }): Observable<Project> {
    return this.http.put<Project>(`/api/projects/${id}`, body);
  }

  members(): Observable<Member[]> {
    return this.http.get<Member[]>('/api/members');
  }

  addMember(body: { email: string; role: string }): Observable<Member> {
    return this.http.post<Member>('/api/members', body);
  }

  changeRole(id: string, role: string): Observable<Member> {
    return this.http.patch<Member>(`/api/members/${id}`, { role });
  }

  tasks(filters: Record<string, string | number | undefined>): Observable<Page<Task>> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) {
      if (value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<Page<Task>>('/api/tasks', { params });
  }

  task(id: string): Observable<Task> {
    return this.http.get<Task>(`/api/tasks/${id}`);
  }

  createTask(body: unknown, idempotencyKey: string): Observable<Task> {
    return this.http.post<Task>('/api/tasks', body, { headers: { 'Idempotency-Key': idempotencyKey } });
  }

  updateTask(id: string, body: unknown): Observable<Task> {
    return this.http.put<Task>(`/api/tasks/${id}`, body);
  }

  deleteTask(id: string): Observable<void> {
    return this.http.delete<void>(`/api/tasks/${id}`);
  }

  dashboard(): Observable<Dashboard> {
    return this.http.get<Dashboard>('/api/dashboard');
  }

  audit(page = 0, size = 20): Observable<Page<AuditEntry>> {
    return this.http.get<Page<AuditEntry>>('/api/audit', { params: { page, size } });
  }
}
