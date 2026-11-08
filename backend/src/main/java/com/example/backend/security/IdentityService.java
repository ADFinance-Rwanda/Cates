package com.example.backend.security;

import com.example.backend.user.UserService;
import com.example.backend.common.exception.ApiException;
import com.example.backend.common.AppRole;
import com.example.backend.organization.Organization;
import com.example.backend.membership.Membership;
import com.example.backend.user.User;
import com.example.backend.membership.MembershipRepository;
import com.example.backend.organization.OrganizationRepository;
import com.example.backend.user.UserRepository;
import com.example.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdentityService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository memberRepository;
    private final UserService userProvisioner;

    @Transactional
    public CurrentUser establish(Jwt jwt, String organizationHeader) {
        User user = userRepository.findByKeycloakSub(jwt.getSubject())
                .orElseGet(() -> userProvisioner.provision(jwt.getSubject(), emailOf(jwt), nameOf(jwt)));
        boolean superAdmin = isSuperAdmin(jwt);
        if (superAdmin) {
            return superAdminPrincipal(user, organizationHeader);
        }
        return memberRepository.findByUserId(user.getId())
                .map(membership -> memberPrincipal(user, membership))
                .orElseGet(() -> unassigned(user));
    }

    private CurrentUser superAdminPrincipal(User user, String organizationHeader) {
        if (organizationHeader == null || organizationHeader.isBlank()) {
            return new CurrentUser(
                    user.getId(), user.getKeycloakSub(), user.getEmail(), user.getDisplayName(),
                    null, null, null, AppRole.SUPER_ADMIN, true);
        }
        UUID organizationId = parseOrganizationId(organizationHeader);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Organization not found"));
        return new CurrentUser(
                user.getId(), user.getKeycloakSub(), user.getEmail(), user.getDisplayName(),
                organization.getId(), organization.getName(), organization.getStatus().name(),
                AppRole.SUPER_ADMIN, true);
    }

    private CurrentUser memberPrincipal(User user, Membership membership) {
        Organization organization = organizationRepository.findById(membership.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Organization not found"));
        AppRole role = switch (membership.getRole()) {
            case ORG_ADMIN -> AppRole.ORG_ADMIN;
            case PROJECT_MANAGER -> AppRole.PROJECT_MANAGER;
            case MEMBER -> AppRole.MEMBER;
        };
        return new CurrentUser(
                user.getId(), user.getKeycloakSub(), user.getEmail(), user.getDisplayName(),
                organization.getId(), organization.getName(), organization.getStatus().name(),
                role, false);
    }

    private CurrentUser unassigned(User user) {
        return new CurrentUser(
                user.getId(), user.getKeycloakSub(), user.getEmail(), user.getDisplayName(),
                null, null, null, AppRole.NONE, false);
    }

    private UUID parseOrganizationId(String value) {
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "X-Organization-Id must be a UUID");
        }
    }

    public static boolean isSuperAdmin(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (!(realmAccess instanceof Map<?, ?> map)) {
            return false;
        }
        Object roles = map.get("roles");
        if (!(roles instanceof Collection<?> collection)) {
            return false;
        }
        return collection.stream().anyMatch(role -> "SUPER_ADMIN".equals(String.valueOf(role)));
    }

    private static String emailOf(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            return email.toLowerCase();
        }
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) {
            return jwt.getSubject() + "@users.taskflow.local";
        }
        return username.toLowerCase() + "@users.taskflow.local";
    }

    private static String nameOf(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        if (name != null && !name.isBlank()) {
            return name;
        }
        String username = jwt.getClaimAsString("preferred_username");
        return username == null || username.isBlank() ? "User" : username;
    }
}
