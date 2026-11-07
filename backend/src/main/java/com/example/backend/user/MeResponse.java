package com.example.backend.user;

import java.util.UUID;

public record MeResponse(
        UUID id,
        String email,
        String displayName,
        boolean superAdmin,
        UUID organizationId,
        String organizationName,
        String organizationStatus,
        String role
) {
}
