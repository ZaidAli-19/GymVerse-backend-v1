package com.company.GymVerse.repositories.tenant;

import com.company.GymVerse.models.tenant.MemberMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for MemberMembership entities.
 */
@Repository
public interface MemberMembershipRepository extends JpaRepository<MemberMembership, Long> {

    List<MemberMembership> findByMemberIdAndActiveTrue(Long memberId);

    List<MemberMembership> findByMemberIdOrderByStartDateDesc(Long memberId);
}
