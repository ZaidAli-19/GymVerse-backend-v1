package com.company.GymVerse.repositories.central;

import com.company.GymVerse.models.central.UserTenantRole;
import com.company.GymVerse.models.central.enums.TenantRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for central UserTenantRole entities.
 */
@Repository
public interface UserTenantRoleRepository extends JpaRepository<UserTenantRole, Long> {

    List<UserTenantRole> findByUserAccountId(Long userAccountId);

    List<UserTenantRole> findByGymTenantId(Long gymTenantId);

    Optional<UserTenantRole> findByUserAccountIdAndGymTenantId(Long userAccountId, Long gymTenantId);

    Optional<UserTenantRole> findByUserAccountIdAndGymTenantIdAndRole(Long userAccountId, Long gymTenantId, TenantRole role);

    boolean existsByUserAccountIdAndGymTenantIdAndRole(Long userAccountId, Long gymTenantId, TenantRole role);
}
