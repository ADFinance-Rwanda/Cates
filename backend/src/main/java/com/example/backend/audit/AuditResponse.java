package com.example.backend.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditResponse(
        UUID id,
        UUID organizationId,
        UUID userId,
        String action,
        String entityType,
        UUID entityId,
        String ipAddress,
        Map<String, Object> metadata,
        Instant createdAt
) {
}
