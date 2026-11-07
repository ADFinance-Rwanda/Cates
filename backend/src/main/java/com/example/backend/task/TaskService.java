package com.example.backend.task;

import com.example.backend.audit.AuditService;
import com.example.backend.dashboard.DashboardCache;
import com.example.backend.security.PermissionService;
import com.example.backend.worker.OutboxService;
import com.example.backend.common.dto.PageResponse;
import com.example.backend.common.exception.ApiException;
import com.example.backend.task.dto.CreateTaskRequest;
import com.example.backend.task.dto.TaskResponse;
import com.example.backend.task.dto.UpdateTaskRequest;
import com.example.backend.common.AppRole;
import com.example.backend.project.ProjectStatus;
import com.example.backend.task.TaskPriority;
import com.example.backend.task.TaskStatus;
import com.example.backend.project.Project;
import com.example.backend.task.Task;
import com.example.backend.idempotency.IdempotencyRecord;
import com.example.backend.idempotency.IdempotencyService;
import com.example.backend.membership.MembershipRepository;
import com.example.backend.project.ProjectRepository;
import com.example.backend.task.TaskRepository;
import com.example.backend.security.CurrentUser;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final IdempotencyService idempotencyService;
    private final MembershipRepository memberRepository;
    private final PermissionService permissions;
    private final AuditService auditService;
    private final OutboxService outboxService;
    private final DashboardCache dashboardCache;
    private final EntityManager entityManager;

    public TaskService(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            IdempotencyService idempotencyService,
            MembershipRepository memberRepository,
            PermissionService permissions,
            AuditService auditService,
            OutboxService outboxService,
            DashboardCache dashboardCache,
            EntityManager entityManager
    ) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.idempotencyService = idempotencyService;
        this.memberRepository = memberRepository;
        this.permissions = permissions;
        this.auditService = auditService;
        this.outboxService = outboxService;
        this.dashboardCache = dashboardCache;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(
            UUID requestedOrganizationId,
            TaskStatus status,
            TaskPriority priority,
            UUID assigneeId,
            UUID projectId,
            LocalDate dueFrom,
            LocalDate dueTo,
            String query,
            int page,
            int size
    ) {
        permissions.rejectForeignTenant(requestedOrganizationId);
        UUID organizationId = permissions.requireOrganization();
        Page<Task> result = taskRepository.findAll(
                TaskSpecifications.filter(
                        organizationId, status, priority, assigneeId, projectId, dueFrom, dueTo,
                        TaskSpecifications.likeContains(query)),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResponse<>(
                result.map(this::toResponse).getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public TaskResponse get(UUID id) {
        return toResponse(loadInTenant(id));
    }

    @Transactional
    public CreateResult create(CreateTaskRequest request, String idempotencyKey) {
        validateKey(idempotencyKey);
        UUID organizationId = permissions.requireActiveOrganization();
        CurrentUser principal = permissions.current();
        lock(organizationId, principal.userId(), idempotencyKey);
        String hash = requestHash(request);
        Optional<IdempotencyRecord> existing = idempotencyService.find(
                organizationId, principal.userId(), idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(hash)) {
                throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED",
                        "This idempotency key was already used with a different request");
            }
            Task task = taskRepository.findByIdAndOrganizationId(existing.get().getTaskId(), organizationId)
                    .orElseThrow(() -> permissions.notFound("Task"));
            return new CreateResult(toResponse(task), true);
        }

        Project project = requireProject(organizationId, request.projectId());
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "UNPROCESSABLE", "Project is archived",
                    Map.of("projectId", "Choose an active project"));
        }
        requireAssignee(organizationId, request.assigneeId());
        TaskStatus status = request.status() == null ? TaskStatus.TODO : request.status();
        TaskPriority priority = request.priority() == null ? TaskPriority.MEDIUM : request.priority();

        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setOrganizationId(organizationId);
        task.setProjectId(project.getId());
        task.setTitle(request.title().trim());
        task.setDescription(blankToNull(request.description()));
        task.setStatus(status);
        task.setPriority(priority);
        task.setOwnerId(principal.userId());
        task.setAssigneeId(request.assigneeId());
        task.setDueDate(request.dueDate());
        if (status == TaskStatus.DONE) {
            task.setCompletedAt(Instant.now());
        }
        taskRepository.saveAndFlush(task);

        IdempotencyRecord key = new IdempotencyRecord();
        key.setId(UUID.randomUUID());
        key.setOrganizationId(organizationId);
        key.setUserId(principal.userId());
        key.setIdempotencyKey(idempotencyKey);
        key.setRequestHash(hash);
        key.setTaskId(task.getId());
        key.setCreatedAt(Instant.now());
        idempotencyService.save(key);

        auditService.record(principal, "TASK_CREATED", "TASK", task.getId(), Map.of("title", task.getTitle()));
        if (task.getAssigneeId() != null) {
            outboxService.enqueue("TASK_ASSIGNED", organizationId, task.getId(), Map.of("taskId", task.getId().toString()));
        }
        dashboardCache.bump(organizationId);
        return new CreateResult(toResponse(task), false);
    }

    @Transactional
    public TaskResponse update(UUID id, UpdateTaskRequest request, String ifMatch) {
        UUID organizationId = permissions.requireActiveOrganization();
        Task task = loadInTenant(id);
        permissions.checkUpdateTask(task);
        if (ifMatch != null && !ifMatch.replace("\"", "").equals(String.valueOf(request.version()))) {
            throw stale();
        }
        if (!Objects.equals(task.getVersion(), request.version())) {
            throw stale();
        }
        CurrentUser principal = permissions.current();
        if (principal.role() == AppRole.MEMBER) {
            if (!Objects.equals(task.getAssigneeId(), request.assigneeId()) || !task.getProjectId().equals(request.projectId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Members cannot reassign or move a task");
            }
        }
        requireProject(organizationId, request.projectId());
        requireAssignee(organizationId, request.assigneeId());
        Map<String, Object> before = snapshot(task);
        UUID previousAssignee = task.getAssigneeId();
        task.setProjectId(request.projectId());
        task.setTitle(request.title().trim());
        task.setDescription(blankToNull(request.description()));
        task.setStatus(request.status());
        task.setPriority(request.priority());
        task.setAssigneeId(request.assigneeId());
        task.setDueDate(request.dueDate());
        if (request.status() == TaskStatus.DONE) {
            if (task.getCompletedAt() == null) {
                task.setCompletedAt(Instant.now());
            }
        } else {
            task.setCompletedAt(null);
        }
        try {
            taskRepository.saveAndFlush(task);
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw stale();
        }
        auditService.record(principal, "TASK_UPDATED", "TASK", task.getId(), Map.of("before", before, "after", snapshot(task)));
        if (!Objects.equals(previousAssignee, task.getAssigneeId()) && task.getAssigneeId() != null) {
            outboxService.enqueue("TASK_ASSIGNED", organizationId, task.getId(), Map.of("taskId", task.getId().toString()));
        }
        dashboardCache.bump(organizationId);
        return toResponse(task);
    }

    @Transactional
    public void delete(UUID id) {
        permissions.checkDeleteTask();
        UUID organizationId = permissions.requireActiveOrganization();
        Task task = loadInTenant(id);
        taskRepository.delete(task);
        auditService.record(permissions.current(), "TASK_DELETED", "TASK", task.getId(), Map.of("title", task.getTitle()));
        dashboardCache.bump(organizationId);
    }

    private Task loadInTenant(UUID id) {
        UUID organizationId = permissions.requireOrganization();
        return taskRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> permissions.notFound("Task"));
    }

    private Project requireProject(UUID organizationId, UUID projectId) {
        return projectRepository.findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> permissions.notFound("Project"));
    }

    private void requireAssignee(UUID organizationId, UUID assigneeId) {
        if (assigneeId == null) {
            return;
        }
        if (!memberRepository.existsByUserIdAndOrganizationId(assigneeId, organizationId)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "UNPROCESSABLE",
                    "Assignee is not a member of this organization",
                    Map.of("assigneeId", "Assignee must belong to the organization"));
        }
    }

    private void lock(UUID organizationId, UUID userId, String idempotencyKey) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:key)")
                .setParameter("key", advisoryKey(organizationId, userId, idempotencyKey))
                .getSingleResult();
    }

    static long advisoryKey(UUID organizationId, UUID userId, String idempotencyKey) {
        byte[] digest = sha256(organizationId + ":" + userId + ":" + idempotencyKey);
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value = (value << 8) | (digest[i] & 0xff);
        }
        return value;
    }

    private String requestHash(CreateTaskRequest request) {
        TaskStatus status = request.status() == null ? TaskStatus.TODO : request.status();
        TaskPriority priority = request.priority() == null ? TaskPriority.MEDIUM : request.priority();
        String canonical = String.join("|",
                request.projectId().toString(),
                request.title().trim(),
                request.description() == null ? "" : request.description().trim(),
                status.name(),
                priority.name(),
                request.assigneeId() == null ? "" : request.assigneeId().toString(),
                request.dueDate() == null ? "" : request.dueDate().toString());
        return HexFormat.of().formatHex(sha256(canonical));
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void validateKey(String idempotencyKey) {
        if (idempotencyKey == null || !idempotencyKey.matches("^[A-Za-z0-9._:-]{1,128}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Idempotency-Key is required",
                    Map.of("Idempotency-Key", "Use 1-128 characters from A-Z, a-z, 0-9, ., _, :, -"));
        }
    }

    private ApiException stale() {
        return new ApiException(HttpStatus.CONFLICT, "STALE_VERSION",
                "The task was updated by someone else. Reload the latest version and try again.");
    }

    private Map<String, Object> snapshot(Task task) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("title", truncate(task.getTitle()));
        values.put("status", task.getStatus().name());
        values.put("priority", task.getPriority().name());
        values.put("projectId", String.valueOf(task.getProjectId()));
        values.put("assigneeId", task.getAssigneeId() == null ? "" : task.getAssigneeId().toString());
        values.put("dueDate", task.getDueDate() == null ? "" : task.getDueDate().toString());
        return values;
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 200) {
            return value;
        }
        return value.substring(0, 200);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getOrganizationId(),
                task.getProjectId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getOwnerId(),
                task.getAssigneeId(),
                task.getDueDate(),
                task.getVersion(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getCompletedAt());
    }

    public record CreateResult(TaskResponse task, boolean replay) {
    }
}
