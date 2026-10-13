package com.company.GymVerse.repositories.tenant;

import com.company.GymVerse.models.tenant.MembershipRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for MembershipRequest entities.
 */
@Repository
public interface MembershipRequestRepository extends JpaRepository<MembershipRequest, Long> {

    List<MembershipRequest> findByUserId(Long userId);

    List<MembershipRequest> findByStatus(String status);
}
