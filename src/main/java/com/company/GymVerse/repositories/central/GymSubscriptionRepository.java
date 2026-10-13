package com.company.GymVerse.repositories.central;

import com.company.GymVerse.models.central.GymSubscription;
import com.company.GymVerse.models.central.enums.GymSubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for central GymSubscription entities.
 */
@Repository
public interface GymSubscriptionRepository extends JpaRepository<GymSubscription, Long> {

    Optional<GymSubscription> findTopByGymTenantIdAndStatusOrderByEndDateDesc(Long gymTenantId, GymSubscriptionStatus status);

    List<GymSubscription> findByGymTenantIdOrderBySubscribedAtDesc(Long gymTenantId);
}
