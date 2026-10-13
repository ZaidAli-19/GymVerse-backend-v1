package com.company.GymVerse.repositories.tenant;

import com.company.GymVerse.models.tenant.JoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for JoinRequest entities.
 */
@Repository
public interface JoinRequestRepository extends JpaRepository<JoinRequest, Long> {

    List<JoinRequest> findByCentralUserId(Long centralUserId);

    List<JoinRequest> findByStatus(String status);
}
