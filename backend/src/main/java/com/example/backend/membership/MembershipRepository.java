package com.example.backend.membership;

import com.example.backend.user.User;
import com.example.backend.membership.MemberResponse;
import com.example.backend.membership.OrgRole;
import com.example.backend.membership.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    Optional<Membership> findByUserId(UUID userId);

    Optional<Membership> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    long countByOrganizationIdAndRole(UUID organizationId, OrgRole role);

    @Query("""
            SELECT new com.example.backend.membership.MemberResponse(
                m.id, u.id, u.email, u.displayName, m.role, m.createdAt)
            FROM Membership m, User u
            WHERE m.userId = u.id AND m.organizationId = :organizationId
            ORDER BY u.displayName ASC
            """)
    List<MemberResponse> findMemberViews(@Param("organizationId") UUID organizationId);
}
