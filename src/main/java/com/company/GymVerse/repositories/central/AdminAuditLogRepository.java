package com.company.GymVerse.repositories.central;

import com.company.GymVerse.models.central.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for central AdminAuditLog entities.
 */
@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    List<AdminAuditLog> findByGymTenantIdOrderByTimestampDesc(Long gymTenantId);
}
