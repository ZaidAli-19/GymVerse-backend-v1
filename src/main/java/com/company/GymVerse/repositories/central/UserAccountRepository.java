package com.company.GymVerse.repositories.central;

import com.company.GymVerse.models.central.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for central UserAccount entities.
 */
@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByIsPlatformAdminTrue();

    Optional<UserAccount> findFirstByIsPlatformAdminTrue();
}
