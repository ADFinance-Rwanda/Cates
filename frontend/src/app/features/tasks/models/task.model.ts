export interface Task {
  id: string;
  organizationId: string;
  projectId: string;
  title: string;
  description: string | null;
  status: 'TODO' | 'IN_PROGRESS' | 'DONE';
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  ownerId: string;
  assigneeId: string | null;
  dueDate: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  completedAt: string | null;
}
