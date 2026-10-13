package com.company.GymVerse.repositories.central;

import com.company.GymVerse.models.central.SaasSubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for central SaasSubscriptionPlan entities.
 */
@Repository
public interface SaasSubscriptionPlanRepository extends JpaRepository<SaasSubscriptionPlan, Long> {

    List<SaasSubscriptionPlan> findByActiveTrue();
}
