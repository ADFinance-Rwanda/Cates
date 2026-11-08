import { Injectable, inject } from '@angular/core';
import { ApiService } from '../../../core/services/api.service';

@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly api = inject(ApiService);

  list(page = 0, size = 20) {
    return this.api.audit(page, size);
  }
}
