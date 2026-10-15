package com.company.GymVerse.repositories.central;

import com.company.GymVerse.models.central.SaasSubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for central SaasSubscriptionPlan entities.
 */
@Repository
public interface SaasSubscriptionPlanRepository extends JpaRepository<SaasSubscriptionPlan, Long>, JpaSpecificationExecutor<SaasSubscriptionPlan> {

    List<SaasSubscriptionPlan> findByActiveTrue();

    boolean existsByNameIgnoreCase(String name);

    Optional<SaasSubscriptionPlan> findByNameIgnoreCase(String name);
}
