package com.company.GymVerse.repositories.tenant;

import com.company.GymVerse.models.tenant.FriendRequest;
import com.company.GymVerse.models.tenant.enums.FriendRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for FriendRequest entities.
 */
@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    Optional<FriendRequest> findBySenderMemberIdAndReceiverMemberId(Long senderId, Long receiverId);

    List<FriendRequest> findByReceiverMemberIdAndStatus(Long receiverId, FriendRequestStatus status);

    Optional<FriendRequest> findBySenderMemberIdAndReceiverMemberIdAndStatus(Long senderId, Long receiverId, FriendRequestStatus status);
}
