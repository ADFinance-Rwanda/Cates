package com.example.backend.task.dto;

import com.example.backend.task.TaskPriority;
import com.example.backend.task.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID organizationId,
        UUID projectId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        UUID ownerId,
        UUID assigneeId,
        LocalDate dueDate,
        Long version,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {
}
