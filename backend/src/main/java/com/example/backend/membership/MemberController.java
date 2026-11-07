package com.example.backend.membership;

import com.example.backend.membership.MemberRequests.AddMemberRequest;
import com.example.backend.membership.MemberRequests.ChangeRoleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Profile("api")
@RequestMapping("/api/members")
@Tag(name = "Members")
public class MemberController {

    private final MembershipService membershipService;

    public MemberController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping
    @Operation(summary = "List members of the caller's organization. Adding or changing roles requires ORG_ADMIN or SUPER_ADMIN.")
    public List<MemberResponse> list() {
        return membershipService.list();
    }

    @PostMapping
    @Operation(summary = "Add an existing user to the caller's organization")
    public MemberResponse add(@Valid @RequestBody AddMemberRequest request) {
        return membershipService.add(request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Change a member role")
    public MemberResponse changeRole(@PathVariable UUID id, @Valid @RequestBody ChangeRoleRequest request) {
        return membershipService.changeRole(id, request);
    }
}
