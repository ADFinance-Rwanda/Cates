package com.example.backend.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobPublisher {

    public static final String STREAM = "jobs";

    private final StringRedisTemplate redis;

    public void publish(UUID outboxId) {
        try {
            redis.opsForStream().add(STREAM, Map.of("outboxId", outboxId.toString()));
        } catch (RuntimeException exception) {
            log.warn("job_enqueue_redis_failed outboxId={}", outboxId);
        }
    }
}
