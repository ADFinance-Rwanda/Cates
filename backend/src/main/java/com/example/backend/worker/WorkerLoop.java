package com.example.backend.worker;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("worker")
@RequiredArgsConstructor
public class WorkerLoop {

    private final BackgroundJobService jobProcessor;
    private final DueSoonScheduler dueSoonScheduler;
    private final IdempotencyPurgeJob idempotencyPurgeJob;

    @Scheduled(fixedDelayString = "${app.worker.poll-delay-ms:5000}")
    public void pollOutbox() {
        jobProcessor.pollPending();
    }

    @Scheduled(fixedDelay = 2000)
    public void readQueue() {
        jobProcessor.readStream();
    }

    @Scheduled(fixedDelay = 60000)
    public void dueSoon() {
        dueSoonScheduler.enqueueDueSoon();
    }

    @Scheduled(cron = "0 15 * * * *")
    public void purgeIdempotencyKeys() {
        idempotencyPurgeJob.purge();
    }
}
