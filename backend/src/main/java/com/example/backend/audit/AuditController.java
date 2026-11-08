package com.example.backend.audit;

import com.example.backend.common.dto.PageResponse;
import com.example.backend.audit.AuditLog;
import com.example.backend.audit.AuditLogRepository;
import com.example.backend.security.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Profile("api")
@RequestMapping("/api/audit")
@Tag(name = "Audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final PermissionService permissions;

    @GetMapping
    @Operation(summary = "Read append-only audit events. ORG_ADMIN and SUPER_ADMIN only. There is no update or delete API.")
    public PageResponse<AuditResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        permissions.checkReadAudit();
        Page<AuditLog> result = auditLogRepository.findByOrganizationIdOrderByCreatedAtDesc(
                permissions.current().organizationId(),
                PageRequest.of(page, size));
        return new PageResponse<>(
                result.map(this::toResponse).getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private AuditResponse toResponse(AuditLog entry) {
        return new AuditResponse(
                entry.getId(),
                entry.getOrganizationId(),
                entry.getUserId(),
                entry.getAction(),
                entry.getEntityType(),
                entry.getEntityId(),
                entry.getIpAddress(),
                entry.getMetadata(),
                entry.getCreatedAt());
    }
}
