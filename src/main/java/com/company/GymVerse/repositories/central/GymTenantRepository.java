package com.company.GymVerse.repositories.central;

import com.company.GymVerse.models.central.GymTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for central GymTenant entities.
 */
@Repository
public interface GymTenantRepository extends JpaRepository<GymTenant, Long>, JpaSpecificationExecutor<GymTenant> {

    Optional<GymTenant> findByGymCode(String gymCode);

    Optional<GymTenant> findByDbName(String dbName);

    boolean existsByGymCode(String gymCode);

    boolean existsByDbName(String dbName);
}
