package com.company.GymVerse.repositories.tenant;

import com.company.GymVerse.models.tenant.GymFacility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for GymFacility entities.
 */
@Repository
public interface GymFacilityRepository extends JpaRepository<GymFacility, Long> {

    Optional<GymFacility> findByNameIgnoreCase(String name);

    List<GymFacility> findAllByOrderByNameAsc();
}
