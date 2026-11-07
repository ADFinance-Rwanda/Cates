package com.example.backend.worker;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@Profile("worker")
@EnableScheduling
@ConditionalOnProperty(name = "app.worker.scheduling", havingValue = "true", matchIfMissing = true)
public class WorkerConfig {
}
