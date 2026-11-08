package com.example.backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import com.example.backend.worker.BackgroundJobService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("worker")
@TestPropertySource(properties = "app.worker.scheduling=false")
class WorkerTest extends AbstractIntegrationTest {

    @Autowired
    private BackgroundJobService jobProcessor;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void assignmentNotificationIsDeliveredOnce() {
        UUID id = insertJob("TASK_ASSIGNED");
        jobProcessor.process(id);
        jobProcessor.process(id);
        assertEquals("DONE", statusOf(id));
        Integer deliveries = jdbc.queryForObject(
                "SELECT count(*) FROM notification_deliveries WHERE outbox_id = ?",
                Integer.class,
                id);
        assertEquals(1, deliveries);
    }

    @Test
    void unknownJobIsRetriedThenDeadLettered() {
        UUID id = insertJob("NOPE");
        for (int i = 0; i < 5; i++) {
            jobProcessor.process(id);
        }
        assertEquals("DEAD", statusOf(id));
        assertEquals(5, attemptsOf(id));
    }

    private UUID insertJob(String type) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO job_outbox (
                    id, job_type, organization_id, entity_id, payload, status, attempts, available_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, '{}'::jsonb, 'PENDING', 0, now(), now(), now())
                """, id, type, UUID.fromString("c2f0c8b6-ab19-475e-ae57-09a4c2c329b6"), UUID.randomUUID());
        return id;
    }

    private String statusOf(UUID id) {
        return jdbc.queryForObject("SELECT status FROM job_outbox WHERE id = ?", String.class, id);
    }

    private int attemptsOf(UUID id) {
        Integer attempts = jdbc.queryForObject("SELECT attempts FROM job_outbox WHERE id = ?", Integer.class, id);
        return attempts == null ? 0 : attempts;
    }
}
