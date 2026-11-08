package com.example.backend.worker;

import com.example.backend.task.Task;
import com.example.backend.task.TaskRepository;
import com.example.backend.worker.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

@Service
@Profile("worker")
@RequiredArgsConstructor
@Slf4j
public class DueSoonScheduler {

    private final TaskRepository taskRepository;
    private final OutboxService outboxService;

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
