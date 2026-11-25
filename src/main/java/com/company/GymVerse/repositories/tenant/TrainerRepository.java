package com.company.GymVerse.repositories.tenant;

import com.company.GymVerse.models.tenant.Trainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for Trainer entities.
 */
@Repository
public interface TrainerRepository extends JpaRepository<Trainer, Long>, JpaSpecificationExecutor<Trainer> {

    Optional<Trainer> findByUserId(Long userId);

    long countByActiveTrue();
}
