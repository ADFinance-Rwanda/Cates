package com.example.backend.security;

import com.example.backend.organization.Organization;
import com.example.backend.common.exception.ApiException;
import com.example.backend.common.exception.ResourceNotFoundException;
import com.example.backend.common.AppRole;
import com.example.backend.task.Task;
import com.example.backend.security.CurrentUser;
import com.example.backend.security.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PermissionService {

    public CurrentUser current() {
        CurrentUser principal = TenantContext.get();
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
        }
        return principal;
    }

    public UUID requireOrganization() {
        CurrentUser principal = current();
        if (!principal.hasOrganization()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "TENANT_REQUIRED",
                    "Choose an organization. Super admins must send X-Organization-Id.");
        }
        return principal.organizationId();
    }

    public UUID requireActiveOrganization() {
        UUID organizationId = requireOrganization();
        CurrentUser principal = current();
        if (!principal.superAdmin() && "SUSPENDED".equals(principal.organizationStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ORGANIZATION_SUSPENDED", "Organization is suspended");
        }
        return organizationId;
    }

    public void checkManageOrganizations() {
        if (!current().superAdmin()) {
            throw forbidden();
        }
    }

    public void checkManageMembers() {
        CurrentUser principal = current();
        requireActiveOrganization();
        if (principal.superAdmin() || principal.role() == AppRole.ORG_ADMIN) {
            return;
        }
        throw forbidden();
    }

    public void checkReadAudit() {
        CurrentUser principal = current();
        requireOrganization();
        if (principal.superAdmin() || principal.role() == AppRole.ORG_ADMIN) {
            return;
        }
        throw forbidden();
    }

    public void checkCreateProject() {
        CurrentUser principal = current();
        requireActiveOrganization();
        if (principal.superAdmin() || principal.role() == AppRole.ORG_ADMIN || principal.role() == AppRole.PROJECT_MANAGER) {
            return;
        }
        throw forbidden();
    }

    public void checkDeleteTask() {
        CurrentUser principal = current();
        requireActiveOrganization();
        if (principal.superAdmin() || principal.role() == AppRole.ORG_ADMIN || principal.role() == AppRole.PROJECT_MANAGER) {
            return;
        }
        throw forbidden();
    }

    public void checkUpdateTask(Task task) {
        CurrentUser principal = current();
        UUID organizationId = requireActiveOrganization();
        if (!organizationId.equals(task.getOrganizationId())) {
            throw notFound("Task");
        }
        if (principal.superAdmin() || principal.role() == AppRole.ORG_ADMIN || principal.role() == AppRole.PROJECT_MANAGER) {
            return;
        }
        if (principal.role() == AppRole.MEMBER
                && (principal.userId().equals(task.getOwnerId()) || principal.userId().equals(task.getAssigneeId()))) {
            return;
        }
        throw forbidden();
    }

    public boolean fullAnalytics() {
        CurrentUser principal = current();
        return principal.superAdmin()
                || principal.role() == AppRole.ORG_ADMIN
                || principal.role() == AppRole.PROJECT_MANAGER;
    }

    public void rejectForeignTenant(UUID requestedOrganizationId) {
        if (requestedOrganizationId == null) {
            return;
        }
        UUID actual = requireOrganization();
        if (!requestedOrganizationId.equals(actual)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Tenant mismatch");
        }
    }

    public ApiException notFound(String resource) {
        return new ResourceNotFoundException(resource);
    }

    private ApiException forbidden() {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to do that");
    }
}
