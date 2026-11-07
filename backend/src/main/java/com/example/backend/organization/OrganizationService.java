package com.example.backend.organization;

import com.example.backend.audit.AuditService;
import com.example.backend.security.PermissionService;
import com.example.backend.organization.OrganizationRequests.CreateOrganizationRequest;
import com.example.backend.organization.OrganizationRequests.OrganizationResponse;
import com.example.backend.organization.OrganizationRequests.UpdateOrganizationRequest;
import com.example.backend.organization.OrgStatus;
import com.example.backend.organization.Organization;
import com.example.backend.organization.OrganizationRepository;
import com.example.backend.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final PermissionService permissions;
    private final AuditService auditService;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            PermissionService permissions,
            AuditService auditService
    ) {
        this.organizationRepository = organizationRepository;
        this.permissions = permissions;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> list() {
        permissions.checkManageOrganizations();
        return organizationRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse get(UUID id) {
        CurrentUser principal = permissions.current();
        if (!principal.superAdmin()) {
            UUID organizationId = permissions.requireOrganization();
            if (!organizationId.equals(id)) {
                throw permissions.notFound("Organization");
            }
        }
        return toResponse(organizationRepository.findById(id).orElseThrow(() -> permissions.notFound("Organization")));
    }

    @Transactional
    public OrganizationResponse create(CreateOrganizationRequest request) {
        permissions.checkManageOrganizations();
        Organization organization = new Organization();
        organization.setId(UUID.randomUUID());
        organization.setName(request.name().trim());
        organization.setStatus(OrgStatus.ACTIVE);
        organizationRepository.save(organization);
        CurrentUser actor = permissions.current();
        auditService.record(
                new CurrentUser(
                        actor.userId(), actor.keycloakSub(), actor.email(), actor.displayName(),
                        organization.getId(), organization.getName(), organization.getStatus().name(),
                        actor.role(), true),
                "ORGANIZATION_CREATED",
                "ORGANIZATION",
                organization.getId(),
                Map.of("name", organization.getName()));
        return toResponse(organization);
    }

    @Transactional
    public OrganizationResponse update(UUID id, UpdateOrganizationRequest request) {
        permissions.checkManageOrganizations();
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> permissions.notFound("Organization"));
        organization.setName(request.name().trim());
        organization.setStatus(request.status());
        CurrentUser actor = permissions.current();
        auditService.record(
                new CurrentUser(
                        actor.userId(), actor.keycloakSub(), actor.email(), actor.displayName(),
                        organization.getId(), organization.getName(), organization.getStatus().name(),
                        actor.role(), true),
                "ORGANIZATION_UPDATED",
                "ORGANIZATION",
                organization.getId(),
                Map.of("status", organization.getStatus().name()));
        return toResponse(organization);
    }

    private OrganizationResponse toResponse(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getStatus(),
                organization.getCreatedAt(),
                organization.getUpdatedAt());
    }
}
