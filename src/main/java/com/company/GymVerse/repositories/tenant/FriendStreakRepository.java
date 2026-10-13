package com.company.GymVerse.repositories.tenant;

import com.company.GymVerse.models.tenant.FriendStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for FriendStreak entities.
 */
@Repository
public interface FriendStreakRepository extends JpaRepository<FriendStreak, Long> {

    Optional<FriendStreak> findByMember1IdAndMember2Id(Long member1Id, Long member2Id);
}
