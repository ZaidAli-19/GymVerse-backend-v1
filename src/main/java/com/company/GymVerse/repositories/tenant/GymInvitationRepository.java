package com.company.GymVerse.repositories.tenant;

import com.company.GymVerse.models.tenant.GymInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for GymInvitation entities.
 * INVARIANT: Performs cryptographic hash-based lookups via invitationTokenHash.
 */
@Repository
public interface GymInvitationRepository extends JpaRepository<GymInvitation, Long> {

    Optional<GymInvitation> findByInvitationTokenHash(String invitationTokenHash);

    List<GymInvitation> findByEmailAndStatus(String email, String status);

    boolean existsByInvitationTokenHash(String invitationTokenHash);
}
