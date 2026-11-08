package com.example.backend.user;

import com.example.backend.security.PermissionService;
import com.example.backend.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("api")
@RequestMapping("/api/me")
@Tag(name = "Current user")
@RequiredArgsConstructor
public class UserController {

    private final PermissionService permissions;

    @GetMapping
    @Operation(summary = "Authenticated user and authorization context")
    public MeResponse me() {
        CurrentUser principal = permissions.current();
        return new MeResponse(
                principal.userId(),
                principal.email(),
                principal.displayName(),
                principal.superAdmin(),
                principal.organizationId(),
                principal.organizationName(),
                principal.organizationStatus(),
                principal.role().name());
    }
}
