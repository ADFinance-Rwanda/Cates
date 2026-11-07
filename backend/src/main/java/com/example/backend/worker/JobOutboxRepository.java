package com.example.backend.worker;

import com.example.backend.worker.JobOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobOutboxRepository extends JpaRepository<JobOutbox, UUID> {
}
