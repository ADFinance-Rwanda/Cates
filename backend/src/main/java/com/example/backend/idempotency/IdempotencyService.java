package com.example.backend.idempotency;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyService {

    private final IdempotencyRepository repository;

    public IdempotencyService(IdempotencyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> find(UUID organizationId, UUID userId, String key) {
        return repository.findByOrganizationIdAndUserIdAndIdempotencyKey(organizationId, userId, key);
    }

    public IdempotencyRecord save(IdempotencyRecord record) {
        return repository.save(record);
    }

    @Transactional
    public int purgeOlderThan(Instant cutoff) {
        return repository.deleteByCreatedAtBefore(cutoff);
    }
}
