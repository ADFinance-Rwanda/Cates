package com.example.backend.security;

import com.example.backend.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiter {

    private static final String LUA = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])
            local member = ARGV[4]
            redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
            local count = redis.call('ZCARD', key)
            if count >= limit then
              return 0
            end
            redis.call('ZADD', key, now, member)
            redis.call('PEXPIRE', key, window)
            return 1
            """;

    private final StringRedisTemplate redis;
    private final AppProperties properties;
    private final DefaultRedisScript<Long> script = rateLimitScript();

    private static DefaultRedisScript<Long> rateLimitScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(LUA);
        script.setResultType(Long.class);
        return script;
    }

    /**
     * Sliding window. Redis failures fail open so task CRUD is not blocked by a cache outage.
     */
    public boolean allow(String subject, String method, String route) {
        AppProperties.RateLimit settings = properties.getRateLimit();
        String key = settings.getKeyPrefix() + ":" + subject + ":" + method + ":" + route;
        long now = System.currentTimeMillis();
        long windowMs = settings.getWindowSeconds() * 1000L;
        try {
            Long result = redis.execute(
                    script,
                    List.of(key),
                    String.valueOf(now),
                    String.valueOf(windowMs),
                    String.valueOf(settings.getLimit()),
                    UUID.randomUUID().toString());
            return result != null && result == 1L;
        } catch (RuntimeException exception) {
            log.warn("rate_limit_unavailable key_prefix={} failing_open=true", settings.getKeyPrefix());
            return true;
        }
    }

    public int windowSeconds() {
        return properties.getRateLimit().getWindowSeconds();
    }
}
