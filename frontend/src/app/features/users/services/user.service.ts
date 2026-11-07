import { Injectable, inject } from '@angular/core';
import { ApiService } from '../../../core/services/api.service';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly api = inject(ApiService);

  list() {
    return this.api.members();
  }

  add(body: { email: string; role: string }) {
    return this.api.addMember(body);
  }

  changeRole(id: string, role: string) {
    return this.api.changeRole(id, role);
  }
}
