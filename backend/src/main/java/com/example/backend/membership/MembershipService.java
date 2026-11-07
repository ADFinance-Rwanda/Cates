package com.example.backend.membership;

import com.example.backend.audit.AuditService;
import com.example.backend.security.PermissionService;
import com.example.backend.common.exception.ApiException;
import com.example.backend.membership.MemberRequests.AddMemberRequest;
import com.example.backend.membership.MemberRequests.ChangeRoleRequest;
import com.example.backend.membership.MemberResponse;
import com.example.backend.membership.OrgRole;
import com.example.backend.membership.Membership;
import com.example.backend.user.User;
import com.example.backend.membership.MembershipRepository;
import com.example.backend.user.UserRepository;
import com.example.backend.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MembershipService {

    private final MembershipRepository memberRepository;
    private final UserRepository userRepository;
    private final PermissionService permissions;
    private final AuditService auditService;

    public MembershipService(
            MembershipRepository memberRepository,
            UserRepository userRepository,
            PermissionService permissions,
            AuditService auditService
    ) {
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.permissions = permissions;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> list() {
        UUID organizationId = permissions.requireOrganization();
        return memberRepository.findMemberViews(organizationId);
    }

    @Transactional
    public MemberResponse add(AddMemberRequest request) {
        permissions.checkManageMembers();
        UUID organizationId = permissions.current().organizationId();
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "UNPROCESSABLE",
                        "User has not signed in yet", Map.of("email", "No local user exists for that email")));
        if (memberRepository.findByUserId(user.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "CONFLICT", "User already belongs to an organization");
        }
        Membership membership = new Membership();
        membership.setId(UUID.randomUUID());
        membership.setUserId(user.getId());
        membership.setOrganizationId(organizationId);
        membership.setRole(request.role());
        memberRepository.save(membership);
        auditService.record(permissions.current(), "USER_ROLE_CHANGED", "MEMBERSHIP", membership.getId(),
                Map.of("email", user.getEmail(), "role", request.role().name()));
        return new MemberResponse(
                membership.getId(), user.getId(), user.getEmail(), user.getDisplayName(), membership.getRole(), membership.getCreatedAt());
    }

    @Transactional
    public MemberResponse changeRole(UUID membershipId, ChangeRoleRequest request) {
        permissions.checkManageMembers();
        UUID organizationId = permissions.current().organizationId();
        Membership membership = memberRepository.findByIdAndOrganizationId(membershipId, organizationId)
                .orElseThrow(() -> permissions.notFound("Member"));
        if (membership.getRole() == OrgRole.ORG_ADMIN && request.role() != OrgRole.ORG_ADMIN) {
            long admins = memberRepository.countByOrganizationIdAndRole(organizationId, OrgRole.ORG_ADMIN);
            if (admins <= 1) {
                throw new ApiException(HttpStatus.CONFLICT, "CONFLICT", "The organization must keep one organization admin");
            }
        }
        OrgRole previous = membership.getRole();
        membership.setRole(request.role());
        CurrentUser actor = permissions.current();
        auditService.record(actor, "USER_ROLE_CHANGED", "MEMBERSHIP", membership.getId(),
                Map.of("from", previous.name(), "to", request.role().name(), "userId", membership.getUserId().toString()));
        User user = userRepository.findById(membership.getUserId()).orElseThrow(() -> permissions.notFound("User"));
        return new MemberResponse(
                membership.getId(), user.getId(), user.getEmail(), user.getDisplayName(), membership.getRole(), membership.getCreatedAt());
    }
}
