package com.company.GymVerse.repositories.tenant;

import com.company.GymVerse.models.tenant.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Member entities.
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByUserId(Long userId);

    List<Member> findByTrainerId(Long trainerId);

    Optional<Member> findByGymId(String gymId);
}
