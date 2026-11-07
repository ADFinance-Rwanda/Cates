import { Injectable, inject } from '@angular/core';
import { ApiService } from '../../../core/services/api.service';

@Injectable({ providedIn: 'root' })
export class ProjectService {
  private readonly api = inject(ApiService);

  list(page = 0, size = 20) {
    return this.api.projects(page, size);
  }

  get(id: string) {
    return this.api.project(id);
  }

  create(body: { name: string; description?: string }) {
    return this.api.createProject(body);
  }

  update(id: string, body: { name: string; description?: string; status: string }) {
    return this.api.updateProject(id, body);
  }
}
