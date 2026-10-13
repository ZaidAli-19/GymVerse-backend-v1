package com.company.GymVerse.repositories.tenant;

import com.company.GymVerse.models.tenant.GymConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for GymConfig entities.
 */
@Repository
public interface GymConfigRepository extends JpaRepository<GymConfig, Long> {

    Optional<GymConfig> findTopByOrderByIdAsc();
}
