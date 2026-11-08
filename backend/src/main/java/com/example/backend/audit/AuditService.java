package com.example.backend.audit;

import com.example.backend.audit.AuditLog;
import com.example.backend.audit.AuditLogRepository;
import com.example.backend.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void record(CurrentUser principal, String action, String entityType, UUID entityId, Map<String, Object> metadata) {
        AuditLog entry = new AuditLog();
        entry.setId(UUID.randomUUID());
        entry.setOrganizationId(principal.organizationId());
        entry.setUserId(principal.userId());
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setIpAddress(clientIp());
        entry.setMetadata(metadata == null ? Map.of() : metadata);
        entry.setCreatedAt(Instant.now());
        auditLogRepository.save(entry);
    }

    private String clientIp() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        return request.getRemoteAddr();
    }
}
