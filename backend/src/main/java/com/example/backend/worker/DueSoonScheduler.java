package com.example.backend.worker;

import com.example.backend.task.Task;
import com.example.backend.task.TaskRepository;
import com.example.backend.worker.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

@Service
@Profile("worker")
public class DueSoonScheduler {

    private static final Logger log = LoggerFactory.getLogger(DueSoonScheduler.class);

    private final TaskRepository taskRepository;
    private final OutboxService outboxService;

    public DueSoonScheduler(TaskRepository taskRepository, OutboxService outboxService) {
        this.taskRepository = taskRepository;
        this.outboxService = outboxService;
    }

    @Transactional
    public int enqueueDueSoon() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        int count = 0;
        for (Task task : taskRepository.findDueForReminder(today, today.plusDays(1))) {
            outboxService.enqueue(
                    "TASK_DUE_SOON",
                    task.getOrganizationId(),
                    task.getId(),
                    Map.of("taskId", task.getId().toString()));
            task.setDueReminderSentAt(Instant.now());
            count++;
        }
        if (count > 0) {
            log.info("due_reminders_enqueued count={}", count);
        }
        return count;
    }
}
