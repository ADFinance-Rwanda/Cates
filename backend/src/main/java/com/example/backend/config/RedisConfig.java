package com.example.backend.config;

import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.net.URI;
import java.time.Duration;

@Configuration
public class RedisConfig {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(DataRedisProperties properties) {
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration();
        boolean ssl = properties.getSsl().isEnabled();
        String url = properties.getUrl();
        if (url != null && !url.isBlank()) {
            URI redisUri = URI.create(url);
            standalone.setHostName(redisUri.getHost());
            standalone.setPort(redisUri.getPort() > 0 ? redisUri.getPort() : 6379);
            applyUserInfo(standalone, redisUri.getUserInfo());
            ssl = ssl || "rediss".equalsIgnoreCase(redisUri.getScheme());
        } else {
            standalone.setHostName(properties.getHost());
            standalone.setPort(properties.getPort());
            String password = properties.getPassword();
            if (password != null && !password.isBlank()) {
                standalone.setPassword(password);
            }
        }
        Duration timeout = properties.getTimeout() == null ? Duration.ofSeconds(2) : properties.getTimeout();
        LettuceClientConfiguration.LettuceClientConfigurationBuilder client = LettuceClientConfiguration.builder()
                .commandTimeout(timeout);
        if (ssl) {
            client.useSsl();
        }
        return new LettuceConnectionFactory(standalone, client.build());
    }

    private static void applyUserInfo(RedisStandaloneConfiguration standalone, String userInfo) {
        if (userInfo == null || userInfo.isBlank()) {
            return;
        }
        int colon = userInfo.indexOf(':');
        String username = colon >= 0 ? userInfo.substring(0, colon) : "";
        String password = colon >= 0 ? userInfo.substring(colon + 1) : userInfo;
        if (!username.isBlank()) {
            standalone.setUsername(username);
        }
        if (!password.isBlank()) {
            standalone.setPassword(password);
        }
    }
}
