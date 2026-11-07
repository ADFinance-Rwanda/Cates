package com.example.backend.membership;

import com.example.backend.membership.OrgRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class MemberRequests {

    private MemberRequests() {
    }

    public record AddMemberRequest(@NotBlank @Email @jakarta.validation.constraints.Size(max = 320) String email,
                                   @NotNull OrgRole role) {
    }

    public record ChangeRoleRequest(@NotNull OrgRole role) {
    }
}
