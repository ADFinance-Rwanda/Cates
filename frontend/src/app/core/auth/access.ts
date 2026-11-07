export interface Me {
  id: string;
  email: string;
  displayName: string;
  superAdmin: boolean;
  organizationId: string | null;
  organizationName: string | null;
  organizationStatus: string | null;
  role: string;
}

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiErrorBody {
  error?: string;
  message?: string;
  details?: Record<string, string>;
}

export function canManageProjects(me: Me | null): boolean {
  if (!me) return false;
  return me.superAdmin || me.role === 'ORG_ADMIN' || me.role === 'PROJECT_MANAGER';
}

export function canDeleteTasks(me: Me | null): boolean {
  return canManageProjects(me);
}

export function canManageMembers(me: Me | null): boolean {
  if (!me) return false;
  return me.superAdmin || me.role === 'ORG_ADMIN';
}
