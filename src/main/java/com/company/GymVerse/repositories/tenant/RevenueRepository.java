package com.company.GymVerse.repositories.tenant;

import com.company.GymVerse.models.tenant.Revenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for Revenue entities.
 */
@Repository
public interface RevenueRepository extends JpaRepository<Revenue, Long> {

    List<Revenue> findByReceivedAtBetween(Instant start, Instant end);

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM Revenue r
            JOIN r.memberMembership mm
            WHERE mm.member.id = :memberId
            """)
    BigDecimal sumTotalPaidByMemberId(@Param("memberId") Long memberId);
}
