package com.company.GymVerse.repositories.tenant;

import com.company.GymVerse.models.tenant.MemberMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Spring Data JPA repository for MemberMembership entities.
 */
@Repository
public interface MemberMembershipRepository extends JpaRepository<MemberMembership, Long> {

    List<MemberMembership> findByMemberIdAndActiveTrue(Long memberId);

    List<MemberMembership> findByMemberIdOrderByStartDateDesc(Long memberId);

    @Query("""
            SELECT mm FROM MemberMembership mm
            JOIN FETCH mm.plan p
            LEFT JOIN FETCH p.planTier
            WHERE mm.member.id IN :memberIds
            ORDER BY mm.active DESC, mm.endDate DESC, mm.startDate DESC, mm.id DESC
            """)
    List<MemberMembership> findAllWithPlanByMemberIdIn(@Param("memberIds") Collection<Long> memberIds);

    @Query("""
            SELECT DISTINCT mm FROM MemberMembership mm
            JOIN FETCH mm.plan p
            LEFT JOIN FETCH p.planTier
            LEFT JOIN FETCH p.facilities
            WHERE mm.member.id = :memberId
            ORDER BY mm.active DESC, mm.endDate DESC, mm.startDate DESC, mm.id DESC
            """)
    List<MemberMembership> findAllWithPlanAndFacilitiesByMemberId(@Param("memberId") Long memberId);

    @Query("""
            SELECT pt.name, pt.themeKey, COUNT(DISTINCT mm.member.id)
            FROM MemberMembership mm
            JOIN mm.plan p
            LEFT JOIN p.planTier pt
            WHERE mm.active = true AND mm.endDate >= :today
            GROUP BY pt.id, pt.name, pt.themeKey
            """)
    List<Object[]> countActiveMembersGroupedByTier(@Param("today") LocalDate today);

    @Query("""
            SELECT COUNT(DISTINCT mm.member.id)
            FROM MemberMembership mm
            WHERE mm.active = true AND mm.endDate BETWEEN :today AND :cutoff
            """)
    long countExpiringSoonMembers(@Param("today") LocalDate today, @Param("cutoff") LocalDate cutoff);
}
