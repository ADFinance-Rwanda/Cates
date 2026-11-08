package com.example.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
@Getter
public class AppProperties {

    private final Security security = new Security();
    private final Cors cors = new Cors();
    private final RateLimit rateLimit = new RateLimit();
    private final Cache cache = new Cache();
    private final Idempotency idempotency = new Idempotency();
    private final Worker worker = new Worker();

    @Getter
    @Setter
    public static class Security {
        private String issuer;
        private String jwkSetUri;
        private String audience;
    }

    @Getter
    @Setter
    public static class Cors {
        private String allowedOrigins = "http://localhost:4200";
    }

    @Getter
    @Setter
    public static class RateLimit {
        private int limit = 60;
        private int windowSeconds = 60;
        private String keyPrefix = "rl";
    }

    @Getter
    @Setter
    public static class Cache {
        private int dashboardTtlSeconds = 60;
    }

    @Getter
    @Setter
    public static class Idempotency {
        private int retentionHours = 24;
    }

    @Getter
    @Setter
    public static class Worker {
        private int pollDelayMs = 5000;
        private int maxAttempts = 5;
    }
}
