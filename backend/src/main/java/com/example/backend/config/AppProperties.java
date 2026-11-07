package com.example.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Security security = new Security();
    private final Cors cors = new Cors();
    private final RateLimit rateLimit = new RateLimit();
    private final Cache cache = new Cache();
    private final Idempotency idempotency = new Idempotency();
    private final Worker worker = new Worker();

    public Security getSecurity() {
        return security;
    }

    public Cors getCors() {
        return cors;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public Cache getCache() {
        return cache;
    }

    public Idempotency getIdempotency() {
        return idempotency;
    }

    public Worker getWorker() {
        return worker;
    }

    public static class Security {
        private String issuer;
        private String jwkSetUri;
        private String audience;

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getJwkSetUri() {
            return jwkSetUri;
        }

        public void setJwkSetUri(String jwkSetUri) {
            this.jwkSetUri = jwkSetUri;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }
    }

    public static class Cors {
        private String allowedOrigins = "http://localhost:4200";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class RateLimit {
        private int limit = 60;
        private int windowSeconds = 60;
        private String keyPrefix = "rl";

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }
    }

    public static class Cache {
        private int dashboardTtlSeconds = 60;

        public int getDashboardTtlSeconds() {
            return dashboardTtlSeconds;
        }

        public void setDashboardTtlSeconds(int dashboardTtlSeconds) {
            this.dashboardTtlSeconds = dashboardTtlSeconds;
        }
    }

    public static class Idempotency {
        private int retentionHours = 24;

        public int getRetentionHours() {
            return retentionHours;
        }

        public void setRetentionHours(int retentionHours) {
            this.retentionHours = retentionHours;
        }
    }

    public static class Worker {
        private int pollDelayMs = 5000;
        private int maxAttempts = 5;

        public int getPollDelayMs() {
            return pollDelayMs;
        }

        public void setPollDelayMs(int pollDelayMs) {
            this.pollDelayMs = pollDelayMs;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
    }
}
