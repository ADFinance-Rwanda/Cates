package com.example.backend.dashboard;

import tools.jackson.databind.ObjectMapper;
import com.example.backend.dashboard.DashboardResponse;
import com.example.backend.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardCache {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final AppProperties properties;

    public long version(UUID organizationId) {
        try {
            String value = redis.opsForValue().get(versionKey(organizationId));
            return value == null ? 0L : Long.parseLong(value);
        } catch (RuntimeException exception) {
            log.warn("dashboard_cache_version_failed orgId={}", organizationId);
            return 0L;
        }
    }

    public void bump(UUID organizationId) {
        try {
            redis.opsForValue().increment(versionKey(organizationId));
        } catch (RuntimeException exception) {
            log.warn("dashboard_cache_invalidate_failed orgId={}", organizationId);
        }
    }

    public String key(UUID organizationId, UUID userId, String scope, long version) {
        return "dashboard:" + organizationId + ":" + userId + ":" + scope + ":" + version;
    }

    public Optional<DashboardResponse> get(String key) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, DashboardResponse.class));
        } catch (Exception exception) {
            log.warn("dashboard_cache_read_failed");
            return Optional.empty();
        }
    }

    public void put(String key, DashboardResponse response) {
        try {
            redis.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(response),
                    Duration.ofSeconds(properties.getCache().getDashboardTtlSeconds()));
        } catch (Exception exception) {
            log.warn("dashboard_cache_write_failed");
        }
    }

    private String versionKey(UUID organizationId) {
        return "dashboard:ver:" + organizationId;
    }
}
