package com.example.backend.task.dto;

import com.example.backend.task.TaskPriority;
import com.example.backend.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateTaskRequest(
        @NotNull Long version,
        @NotNull UUID projectId,
        @NotBlank @Size(max = 300) String title,
        @Size(max = 5000) String description,
        @NotNull TaskStatus status,
        @NotNull TaskPriority priority,
        UUID assigneeId,
        LocalDate dueDate
) {
}
