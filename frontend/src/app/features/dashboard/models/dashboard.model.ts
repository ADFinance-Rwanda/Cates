export interface Dashboard {
  tasksByStatus: Record<string, number>;
  tasksByPriority: Record<string, number>;
  productivity: {
    totalTasks: number;
    completedTasks: number;
    pendingTasks: number;
    completionRate: number;
    overdueTasks: number;
  };
  trend: { date: string; created: number; completed: number }[];
  scope: 'ORGANIZATION' | 'ASSIGNED';
}
