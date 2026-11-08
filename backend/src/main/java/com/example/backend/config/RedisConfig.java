package com.example.backend.config;

import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(DataRedisProperties properties) {
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration();
        standalone.setHostName(properties.getHost());
        standalone.setPort(properties.getPort());
        String password = properties.getPassword();
        if (password != null && !password.isBlank()) {
            standalone.setPassword(password);
        }
        Duration timeout = properties.getTimeout() == null ? Duration.ofSeconds(2) : properties.getTimeout();
        LettuceClientConfiguration client = LettuceClientConfiguration.builder()
                .commandTimeout(timeout)
                .build();
        return new LettuceConnectionFactory(standalone, client);
    }
}
