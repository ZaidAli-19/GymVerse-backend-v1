package com.company.GymVerse.repositories.tenant;

import com.company.GymVerse.models.tenant.Revenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for Revenue entities.
 */
@Repository
public interface RevenueRepository extends JpaRepository<Revenue, Long> {

    List<Revenue> findByReceivedAtBetween(Instant start, Instant end);
}
