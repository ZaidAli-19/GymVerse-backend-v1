package com.company.GymVerse.repositories.tenant;

import com.company.GymVerse.models.tenant.TenantUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for TenantUser entities.
 */
@Repository
public interface TenantUserRepository extends JpaRepository<TenantUser, Long> {

    Optional<TenantUser> findByCentralUserId(Long centralUserId);

    Optional<TenantUser> findByEmail(String email);

    boolean existsByCentralUserId(Long centralUserId);

    boolean existsByEmail(String email);
}
