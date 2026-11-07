package com.example.backend.task;

import com.example.backend.task.TaskStatus;
import com.example.backend.task.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    Optional<Task> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Query("""
            SELECT t FROM Task t
            WHERE t.dueDate IS NOT NULL
              AND t.dueDate >= :start
              AND t.dueDate <= :end
              AND t.status <> com.example.backend.task.TaskStatus.DONE
              AND t.dueReminderSentAt IS NULL
            """)
    List<Task> findDueForReminder(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(value = """
            SELECT t.status AS status, COUNT(*) AS total
            FROM tasks t
            WHERE t.organization_id = :orgId
              AND (CAST(:memberId AS uuid) IS NULL OR t.owner_id = :memberId OR t.assignee_id = :memberId)
            GROUP BY t.status
            """, nativeQuery = true)
    List<StatusCount> countByStatus(@Param("orgId") UUID orgId, @Param("memberId") UUID memberId);

    @Query(value = """
            SELECT t.priority AS priority, COUNT(*) AS total
            FROM tasks t
            WHERE t.organization_id = :orgId
              AND (CAST(:memberId AS uuid) IS NULL OR t.owner_id = :memberId OR t.assignee_id = :memberId)
            GROUP BY t.priority
            """, nativeQuery = true)
    List<PriorityCount> countByPriority(@Param("orgId") UUID orgId, @Param("memberId") UUID memberId);

    @Query(value = """
            SELECT COUNT(*)
            FROM tasks t
            WHERE t.organization_id = :orgId
              AND t.due_date < CURRENT_DATE
              AND t.status <> 'DONE'
              AND (CAST(:memberId AS uuid) IS NULL OR t.owner_id = :memberId OR t.assignee_id = :memberId)
            """, nativeQuery = true)
    long countOverdue(@Param("orgId") UUID orgId, @Param("memberId") UUID memberId);

    @Query(value = """
            SELECT to_char(t.created_at AT TIME ZONE 'UTC', 'YYYY-MM-DD') AS day, COUNT(*) AS total
            FROM tasks t
            WHERE t.organization_id = :orgId
              AND t.created_at >= :from
              AND (CAST(:memberId AS uuid) IS NULL OR t.owner_id = :memberId OR t.assignee_id = :memberId)
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<DayCount> countCreatedByDay(@Param("orgId") UUID orgId, @Param("memberId") UUID memberId, @Param("from") java.time.Instant from);

    @Query(value = """
            SELECT to_char(t.completed_at AT TIME ZONE 'UTC', 'YYYY-MM-DD') AS day, COUNT(*) AS total
            FROM tasks t
            WHERE t.organization_id = :orgId
              AND t.completed_at IS NOT NULL
              AND t.completed_at >= :from
              AND (CAST(:memberId AS uuid) IS NULL OR t.owner_id = :memberId OR t.assignee_id = :memberId)
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<DayCount> countCompletedByDay(@Param("orgId") UUID orgId, @Param("memberId") UUID memberId, @Param("from") java.time.Instant from);

    interface StatusCount {
        String getStatus();

        long getTotal();
    }

    interface PriorityCount {
        String getPriority();

        long getTotal();
    }

    interface DayCount {
        String getDay();

        long getTotal();
    }

    @Query("SELECT COUNT(t) FROM Task t WHERE t.organizationId = :orgId AND t.status = :status")
    long countByOrganizationAndStatus(@Param("orgId") UUID orgId, @Param("status") TaskStatus status);
}
