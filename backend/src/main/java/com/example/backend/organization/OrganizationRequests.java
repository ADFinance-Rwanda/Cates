package com.example.backend.organization;

import com.example.backend.organization.OrgStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class OrganizationRequests {

    private OrganizationRequests() {
    }

    public record CreateOrganizationRequest(@NotBlank @Size(max = 200) String name) {
    }

    public record UpdateOrganizationRequest(@NotBlank @Size(max = 200) String name, @NotNull OrgStatus status) {
    }

    public record OrganizationResponse(
            UUID id,
            String name,
            OrgStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
