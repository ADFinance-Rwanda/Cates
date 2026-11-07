package com.example.backend.organization;

import com.example.backend.organization.OrganizationRequests.CreateOrganizationRequest;
import com.example.backend.organization.OrganizationRequests.OrganizationResponse;
import com.example.backend.organization.OrganizationRequests.UpdateOrganizationRequest;
import com.example.backend.organization.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Profile("api")
@RequestMapping("/api/organizations")
@Tag(name = "Organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    @Operation(summary = "List organizations", description = "SUPER_ADMIN only.")
    public List<OrganizationResponse> list() {
        return organizationService.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an organization. Other tenants receive 404.")
    public OrganizationResponse get(@PathVariable UUID id) {
        return organizationService.get(id);
    }

    @PostMapping
    @Operation(summary = "Create an organization", description = "SUPER_ADMIN only.")
    public OrganizationResponse create(@Valid @RequestBody CreateOrganizationRequest request) {
        return organizationService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an organization", description = "SUPER_ADMIN only.")
    public OrganizationResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateOrganizationRequest request) {
        return organizationService.update(id, request);
    }
}
