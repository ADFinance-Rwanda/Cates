package com.example.backend.dashboard;

import java.util.List;
import java.util.Map;

public record DashboardResponse(
        Map<String, Long> tasksByStatus,
        Map<String, Long> tasksByPriority,
        Productivity productivity,
        List<TrendPoint> trend,
        String scope
) {
    public record Productivity(
            long totalTasks,
            long completedTasks,
            long pendingTasks,
            double completionRate,
            long overdueTasks
    ) {
    }

    public record TrendPoint(String date, long created, long completed) {
    }
}
