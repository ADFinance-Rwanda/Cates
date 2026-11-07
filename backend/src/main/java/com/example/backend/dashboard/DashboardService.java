package com.example.backend.dashboard;

import com.example.backend.security.PermissionService;
import com.example.backend.dashboard.DashboardResponse.Productivity;
import com.example.backend.dashboard.DashboardResponse.TrendPoint;
import com.example.backend.task.TaskPriority;
import com.example.backend.task.TaskStatus;
import com.example.backend.task.TaskRepository;
import com.example.backend.task.TaskRepository.DayCount;
import com.example.backend.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DashboardService {

    private static final int TREND_DAYS = 14;

    private final TaskRepository taskRepository;
    private final PermissionService permissions;
    private final DashboardCache dashboardCache;

    public DashboardService(TaskRepository taskRepository, PermissionService permissions, DashboardCache dashboardCache) {
        this.taskRepository = taskRepository;
        this.permissions = permissions;
        this.dashboardCache = dashboardCache;
    }

    @Transactional(readOnly = true)
    public DashboardResponse get() {
        UUID organizationId = permissions.requireOrganization();
        CurrentUser principal = permissions.current();
        boolean full = permissions.fullAnalytics();
        UUID memberScope = full ? null : principal.userId();
        String scope = full ? "ORGANIZATION" : "ASSIGNED";
        long version = dashboardCache.version(organizationId);
        String key = dashboardCache.key(organizationId, principal.userId(), scope, version);
        return dashboardCache.get(key).orElseGet(() -> {
            DashboardResponse fresh = compute(organizationId, memberScope, scope);
            dashboardCache.put(key, fresh);
            return fresh;
        });
    }

    private DashboardResponse compute(UUID organizationId, UUID memberScope, String scope) {
        Map<String, Long> byStatus = zeroStatuses();
        taskRepository.countByStatus(organizationId, memberScope)
                .forEach(row -> byStatus.put(row.getStatus(), row.getTotal()));
        Map<String, Long> byPriority = zeroPriorities();
        taskRepository.countByPriority(organizationId, memberScope)
                .forEach(row -> byPriority.put(row.getPriority(), row.getTotal()));

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long completed = byStatus.getOrDefault(TaskStatus.DONE.name(), 0L);
        long overdue = taskRepository.countOverdue(organizationId, memberScope);
        double rate = total == 0 ? 0d : Math.round((completed * 10000d) / total) / 10000d;
        Productivity productivity = new Productivity(total, completed, total - completed, rate, overdue);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate start = today.minusDays(TREND_DAYS - 1L);
        Instant from = start.atStartOfDay().toInstant(ZoneOffset.UTC);
        Map<String, Long> created = index(taskRepository.countCreatedByDay(organizationId, memberScope, from));
        Map<String, Long> done = index(taskRepository.countCompletedByDay(organizationId, memberScope, from));
        List<TrendPoint> trend = new ArrayList<>();
        for (int i = 0; i < TREND_DAYS; i++) {
            String day = start.plusDays(i).toString();
            trend.add(new TrendPoint(day, created.getOrDefault(day, 0L), done.getOrDefault(day, 0L)));
        }
        return new DashboardResponse(byStatus, byPriority, productivity, trend, scope);
    }

    private Map<String, Long> zeroStatuses() {
        Map<String, Long> values = new LinkedHashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            values.put(status.name(), 0L);
        }
        return values;
    }

    private Map<String, Long> zeroPriorities() {
        Map<String, Long> ordered = new LinkedHashMap<>();
        for (TaskPriority priority : TaskPriority.values()) {
            ordered.put(priority.name(), 0L);
        }
        return ordered;
    }

    private Map<String, Long> index(List<DayCount> rows) {
        Map<String, Long> values = new LinkedHashMap<>();
        for (DayCount row : rows) {
            values.put(row.getDay(), row.getTotal());
        }
        return values;
    }
}
