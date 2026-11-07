package com.example.backend.task;

import com.example.backend.common.dto.PageResponse;
import com.example.backend.task.dto.CreateTaskRequest;
import com.example.backend.task.dto.TaskResponse;
import com.example.backend.task.dto.UpdateTaskRequest;
import com.example.backend.task.TaskPriority;
import com.example.backend.task.TaskStatus;
import com.example.backend.task.TaskService;
import com.example.backend.task.TaskService.CreateResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@Validated
@Profile("api")
@RequestMapping("/api/tasks")
@Tag(name = "Tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @Operation(summary = "List tasks in the caller's organization")
    public PageResponse<TaskResponse> list(
            @RequestParam(name = "organization_id", required = false) UUID organizationId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) UUID assignee,
            @RequestParam(required = false) UUID project,
            @RequestParam(required = false) LocalDate dueFrom,
            @RequestParam(required = false) LocalDate dueTo,
            @RequestParam(name = "q", required = false) @Size(max = 100) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return taskService.list(organizationId, status, priority, assignee, project, dueFrom, dueTo, query, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a task. Cross-tenant ids return 404.")
    public ResponseEntity<TaskResponse> get(@PathVariable UUID id) {
        TaskResponse task = taskService.get(id);
        return ResponseEntity.ok().eTag(String.valueOf(task.version())).body(task);
    }

    @PostMapping
    @Operation(summary = "Create a task. Requires Idempotency-Key.")
    public ResponseEntity<TaskResponse> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        CreateResult result = taskService.create(request, idempotencyKey);
        ResponseEntity.BodyBuilder builder = result.replay()
                ? ResponseEntity.ok().header("Idempotent-Replay", "true")
                : ResponseEntity.created(URI.create("/api/tasks/" + result.task().id()));
        return builder.eTag(String.valueOf(result.task().version())).body(result.task());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a task. Send the current version. A stale version returns 409.")
    public ResponseEntity<TaskResponse> update(
            @PathVariable UUID id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        TaskResponse task = taskService.update(id, request, ifMatch);
        return ResponseEntity.ok().eTag(String.valueOf(task.version())).body(task);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task", description = "ORG_ADMIN, PROJECT_MANAGER, or SUPER_ADMIN. Members receive 403.")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
