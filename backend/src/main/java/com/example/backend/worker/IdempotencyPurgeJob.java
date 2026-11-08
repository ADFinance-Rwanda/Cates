package com.example.backend.worker;

import com.example.backend.config.AppProperties;
import com.example.backend.idempotency.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@Profile("worker")
@RequiredArgsConstructor
@Slf4j
public class IdempotencyPurgeJob {

    private final IdempotencyService idempotencyService;
    private final AppProperties properties;

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
