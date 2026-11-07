package com.example.backend.security;

import com.example.backend.common.AppRole;

import java.util.UUID;

public record CurrentUser(
        UUID userId,
        String keycloakSub,
        String email,
        String displayName,
        UUID organizationId,
        String organizationName,
        String organizationStatus,
        AppRole role,
        boolean superAdmin
) {
    public boolean hasOrganization() {
        return organizationId != null;
    }
}
