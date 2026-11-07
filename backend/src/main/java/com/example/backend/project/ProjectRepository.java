package com.example.backend.project;

import com.example.backend.project.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Page<Project> findByOrganizationId(UUID organizationId, Pageable pageable);
}
