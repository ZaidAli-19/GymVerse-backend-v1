package com.company.GymVerse.repositories.tenant;

import com.company.GymVerse.models.tenant.PlanTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for PlanTier entities.
 */
@Repository
public interface PlanTierRepository extends JpaRepository<PlanTier, Long> {

    Optional<PlanTier> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<PlanTier> findAllByOrderByDefaultTierDescNameAsc();
}
