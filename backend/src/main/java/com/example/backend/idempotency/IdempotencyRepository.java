package com.example.backend.idempotency;

import com.example.backend.idempotency.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByOrganizationIdAndUserIdAndIdempotencyKey(
            UUID organizationId, UUID userId, String idempotencyKey);

    @Modifying
    @Query("DELETE FROM IdempotencyRecord k WHERE k.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") Instant cutoff);
}
