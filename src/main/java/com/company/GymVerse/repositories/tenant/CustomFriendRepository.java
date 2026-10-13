package com.company.GymVerse.repositories.tenant;

import com.company.GymVerse.models.tenant.CustomFriend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for CustomFriend entities.
 */
@Repository
public interface CustomFriendRepository extends JpaRepository<CustomFriend, Long> {

    List<CustomFriend> findByUserMemberId(Long userMemberId);
}
