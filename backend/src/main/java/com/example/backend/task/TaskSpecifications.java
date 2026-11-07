package com.example.backend.task;

import com.example.backend.task.TaskPriority;
import com.example.backend.task.TaskStatus;
import com.example.backend.task.Task;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<Task> filter(
            UUID organizationId,
            TaskStatus status,
            TaskPriority priority,
            UUID assigneeId,
            UUID projectId,
            LocalDate dueFrom,
            LocalDate dueTo,
            String query
    ) {
        return (root, criteria, builder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("organizationId"), organizationId));
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(builder.equal(root.get("priority"), priority));
            }
            if (assigneeId != null) {
                predicates.add(builder.equal(root.get("assigneeId"), assigneeId));
            }
            if (projectId != null) {
                predicates.add(builder.equal(root.get("projectId"), projectId));
            }
            if (dueFrom != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("dueDate"), dueFrom));
            }
            if (dueTo != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("dueDate"), dueTo));
            }
            if (query != null) {
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("title")), query, '\\'),
                        builder.like(builder.lower(builder.coalesce(root.get("description"), "")), query, '\\')));
            }
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    public static String likeContains(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String escaped = raw.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
