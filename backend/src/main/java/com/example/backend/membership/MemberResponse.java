package com.example.backend.membership;

import com.example.backend.membership.OrgRole;

import java.time.Instant;
import java.util.UUID;

public record MemberResponse(
        UUID id,
        UUID userId,
        String email,
        String displayName,
        OrgRole role,
        Instant createdAt
) {
}
