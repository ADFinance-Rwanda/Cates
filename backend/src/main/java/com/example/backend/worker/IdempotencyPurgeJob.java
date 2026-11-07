package com.example.backend.worker;

import com.example.backend.config.AppProperties;
import com.example.backend.idempotency.IdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@Profile("worker")
public class IdempotencyPurgeJob {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyPurgeJob.class);

    private final IdempotencyService idempotencyService;
    private final AppProperties properties;

    public IdempotencyPurgeJob(IdempotencyService idempotencyService, AppProperties properties) {
        this.idempotencyService = idempotencyService;
        this.properties = properties;
    }

    @Transactional
    public int purge() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(properties.getIdempotency().getRetentionHours()));
        int removed = idempotencyService.purgeOlderThan(cutoff);
        if (removed > 0) {
            log.info("idempotency_keys_purged count={}", removed);
        }
        return removed;
    }
}
