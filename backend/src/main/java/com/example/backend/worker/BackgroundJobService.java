package com.example.backend.worker;

import com.example.backend.config.AppProperties;
import com.example.backend.worker.JobPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Profile("worker")
@RequiredArgsConstructor
@Slf4j
public class BackgroundJobService {

    private static final String GROUP = "workers";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final StringRedisTemplate redis;
    private final AppProperties properties;
    private final String consumerName = "worker-" + UUID.randomUUID().toString().substring(0, 8);
    private volatile boolean groupReady;

    public void pollPending() {
        List<UUID> ids = jdbcTemplate.query("""
                SELECT id FROM job_outbox
                WHERE status = 'PENDING' AND available_at <= now()
                ORDER BY created_at
                LIMIT 20
                """, (rs, rowNum) -> rs.getObject("id", UUID.class));
        for (UUID id : ids) {
            process(id);
        }
    }

    public void readStream() {
        try {
            ensureGroup();
            List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
                    Consumer.from(GROUP, consumerName),
                    StreamReadOptions.empty().count(10).block(Duration.ofSeconds(1)),
                    StreamOffset.create(JobPublisher.STREAM, ReadOffset.lastConsumed()));
            if (records == null) {
                return;
            }
            for (MapRecord<String, Object, Object> record : records) {
                Object rawId = record.getValue().get("outboxId");
                if (rawId != null) {
                    process(UUID.fromString(rawId.toString()));
                }
                redis.opsForStream().acknowledge(JobPublisher.STREAM, GROUP, record.getId());
            }
        } catch (RuntimeException exception) {
            log.warn("redis_stream_unavailable");
        }
    }

    public void process(UUID outboxId) {
        transactionTemplate.executeWithoutResult(status -> processLocked(outboxId));
    }

    private void processLocked(UUID outboxId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, job_type, organization_id, entity_id, attempts, status
                FROM job_outbox
                WHERE id = ?
                FOR UPDATE
                """, outboxId);
        if (rows.isEmpty()) {
            return;
        }
        Map<String, Object> row = rows.get(0);
        if (!"PENDING".equals(row.get("status"))) {
            return;
        }
        int attempt = ((Number) row.get("attempts")).intValue() + 1;
        String type = String.valueOf(row.get("job_type"));
        UUID organizationId = (UUID) row.get("organization_id");
        UUID entityId = (UUID) row.get("entity_id");
        try {
            dispatch(outboxId, type, organizationId, entityId);
            jdbcTemplate.update("""
                    UPDATE job_outbox
                    SET status = 'DONE', attempts = ?, updated_at = now(), last_error = NULL
                    WHERE id = ?
                    """, attempt, outboxId);
            log.info("job_processed outboxId={} type={} orgId={} attempt={}", outboxId, type, organizationId, attempt);
        } catch (RuntimeException exception) {
            String message = sanitize(exception);
            if (attempt >= properties.getWorker().getMaxAttempts()) {
                jdbcTemplate.update("""
                        UPDATE job_outbox
                        SET status = 'DEAD', attempts = ?, last_error = ?, updated_at = now()
                        WHERE id = ?
                        """, attempt, message, outboxId);
                deadLetter(outboxId, message);
                log.error("job_dead outboxId={} type={} attempt={}", outboxId, type, attempt);
            } else {
                Instant availableAt = Instant.now().plus(backoff(attempt));
                jdbcTemplate.update("""
                        UPDATE job_outbox
                        SET status = 'PENDING', attempts = ?, available_at = ?, last_error = ?, updated_at = now()
                        WHERE id = ?
                        """, attempt, Timestamp.from(availableAt), message, outboxId);
                log.warn("job_retry outboxId={} type={} attempt={}", outboxId, type, attempt);
            }
        }
    }

    private void dispatch(UUID outboxId, String type, UUID organizationId, UUID entityId) {
        switch (type) {
            case "TASK_ASSIGNED", "TASK_DUE_SOON" -> deliver(outboxId, type, organizationId, entityId);
            default -> throw new IllegalStateException("Unknown job type");
        }
    }

    private void deliver(UUID outboxId, String type, UUID organizationId, UUID entityId) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO notification_deliveries (outbox_id, channel, delivered_at)
                VALUES (?, 'LOG', now())
                ON CONFLICT (outbox_id) DO NOTHING
                """, outboxId);
        if (inserted == 0) {
            log.info("notification_duplicate outboxId={}", outboxId);
            return;
        }
        log.info("notification_sent type={} outboxId={} orgId={} entityId={}", type, outboxId, organizationId, entityId);
    }

    private void deadLetter(UUID outboxId, String message) {
        try {
            redis.opsForStream().add("jobs:dlq", Map.of("outboxId", outboxId.toString(), "error", message));
        } catch (RuntimeException exception) {
            log.warn("dead_letter_publish_failed outboxId={}", outboxId);
        }
    }

    private void ensureGroup() {
        if (groupReady) {
            return;
        }
        try {
            redis.execute((RedisCallback<Object>) connection -> connection.execute(
                    "XGROUP",
                    "CREATE".getBytes(StandardCharsets.UTF_8),
                    JobPublisher.STREAM.getBytes(StandardCharsets.UTF_8),
                    GROUP.getBytes(StandardCharsets.UTF_8),
                    "$".getBytes(StandardCharsets.UTF_8),
                    "MKSTREAM".getBytes(StandardCharsets.UTF_8)));
            groupReady = true;
        } catch (DataAccessException exception) {
            groupReady = true;
        }
    }

    static Duration backoff(int attempt) {
        return switch (attempt) {
            case 1 -> Duration.ofSeconds(5);
            case 2 -> Duration.ofSeconds(15);
            case 3 -> Duration.ofSeconds(45);
            default -> Duration.ofMinutes(2);
        };
    }

    private String sanitize(RuntimeException exception) {
        String message = exception.getClass().getSimpleName();
        if (exception.getMessage() != null) {
            String text = exception.getMessage().replaceAll("\\s+", " ");
            if (text.length() > 180) {
                text = text.substring(0, 180);
            }
            message = message + ": " + text;
        }
        return message;
    }
}
