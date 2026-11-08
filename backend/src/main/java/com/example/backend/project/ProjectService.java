package com.example.backend.project;

import com.example.backend.audit.AuditService;
import com.example.backend.security.PermissionService;
import com.example.backend.common.dto.PageResponse;
import com.example.backend.project.dto.CreateProjectRequest;
import com.example.backend.project.dto.ProjectResponse;
import com.example.backend.project.dto.UpdateProjectRequest;
import com.example.backend.project.ProjectStatus;
import com.example.backend.project.Project;
import com.example.backend.project.ProjectRepository;
import com.example.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final PermissionService permissions;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> list(UUID requestedOrganizationId, int page, int size) {
        permissions.rejectForeignTenant(requestedOrganizationId);
        UUID organizationId = permissions.requireOrganization();
        Page<Project> result = projectRepository.findByOrganizationId(
                organizationId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResponse<>(
                result.map(this::toResponse).getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID id) {
        UUID organizationId = permissions.requireOrganization();
        return toResponse(projectRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> permissions.notFound("Project")));
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        permissions.checkCreateProject();
        CurrentUser principal = permissions.current();
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setOrganizationId(principal.organizationId());
        project.setName(request.name().trim());
        project.setDescription(blankToNull(request.description()));
        project.setStatus(ProjectStatus.ACTIVE);
        projectRepository.save(project);
        auditService.record(principal, "PROJECT_CREATED", "PROJECT", project.getId(), Map.of("name", project.getName()));
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse update(UUID id, UpdateProjectRequest request) {
        permissions.checkCreateProject();
        UUID organizationId = permissions.requireActiveOrganization();
        Project project = projectRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> permissions.notFound("Project"));
        project.setName(request.name().trim());
        project.setDescription(blankToNull(request.description()));
        project.setStatus(request.status());
        auditService.record(permissions.current(), "PROJECT_UPDATED", "PROJECT", project.getId(), Map.of("name", project.getName()));
        return toResponse(project);
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getOrganizationId(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
