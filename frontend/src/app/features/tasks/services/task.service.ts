import { Injectable, inject } from '@angular/core';
import { ApiService } from '../../../core/services/api.service';

@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly api = inject(ApiService);

  list(filters: Record<string, string | number | undefined>) {
    return this.api.tasks(filters);
  }

  get(id: string) {
    return this.api.task(id);
  }

  create(body: unknown, idempotencyKey: string) {
    return this.api.createTask(body, idempotencyKey);
  }

  update(id: string, body: unknown) {
    return this.api.updateTask(id, body);
  }

  delete(id: string) {
    return this.api.deleteTask(id);
  }
}
