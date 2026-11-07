package com.example.backend.common;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final RedisConnectionFactory redisConnectionFactory;

    public HealthController(JdbcTemplate jdbcTemplate, RedisConnectionFactory redisConnectionFactory) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    /**
     * Postgres is required. Redis is reported but does not remove the API from rotation,
     * because task CRUD does not depend on it.
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        boolean postgres = pingPostgres();
        boolean redis = pingRedis();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", postgres ? "UP" : "DOWN");
        body.put("components", Map.of("postgres", postgres ? "UP" : "DOWN", "redis", redis ? "UP" : "DOWN"));
        return ResponseEntity.status(postgres ? 200 : 503).body(body);
    }

    private boolean pingPostgres() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return result != null && result == 1;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean pingRedis() {
        RedisConnection connection = null;
        try {
            connection = redisConnectionFactory.getConnection();
            return "PONG".equalsIgnoreCase(connection.ping());
        } catch (RuntimeException exception) {
            return false;
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }
}
