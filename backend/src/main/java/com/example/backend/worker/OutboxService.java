package com.example.backend.worker;

import com.example.backend.worker.JobOutbox;
import com.example.backend.worker.JobOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final JobOutboxRepository jobOutboxRepository;
    private final JobPublisher jobPublisher;

    public UUID enqueue(String jobType, UUID organizationId, UUID entityId, Map<String, Object> payload) {
        Instant now = Instant.now();
        JobOutbox job = new JobOutbox();
        job.setId(UUID.randomUUID());
        job.setJobType(jobType);
        job.setOrganizationId(organizationId);
        job.setEntityId(entityId);
        job.setPayload(payload == null ? Map.of() : payload);
        job.setStatus("PENDING");
        job.setAttempts(0);
        job.setAvailableAt(now);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        jobOutboxRepository.save(job);
        UUID id = job.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    jobPublisher.publish(id);
                }
            });
        } else {
            jobPublisher.publish(id);
        }
        return id;
    }
}
