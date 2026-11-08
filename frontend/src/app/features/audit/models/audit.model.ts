export interface AuditEntry {
  id: string;
  organizationId: string;
  userId: string | null;
  action: string;
  entityType: string;
  entityId: string | null;
  ipAddress: string | null;
  metadata: Record<string, unknown> | null;
  createdAt: string;
}
