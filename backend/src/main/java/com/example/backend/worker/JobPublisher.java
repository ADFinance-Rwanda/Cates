package com.example.backend.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class JobPublisher {

    private static final Logger log = LoggerFactory.getLogger(JobPublisher.class);
    public static final String STREAM = "jobs";

    private final StringRedisTemplate redis;

    public JobPublisher(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void publish(UUID outboxId) {
        try {
            redis.opsForStream().add(STREAM, Map.of("outboxId", outboxId.toString()));
        } catch (RuntimeException exception) {
            log.warn("job_enqueue_redis_failed outboxId={}", outboxId);
        }
    }
}
