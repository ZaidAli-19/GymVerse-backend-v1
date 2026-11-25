package com.company.GymVerse.repositories.tenant;

import com.company.GymVerse.models.tenant.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Member entities.
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, Long>, JpaSpecificationExecutor<Member> {

    Optional<Member> findByUserId(Long userId);

    List<Member> findByTrainerId(Long trainerId);

    Optional<Member> findByGymId(String gymId);

    boolean existsByGymId(String gymId);

    @Query("""
            SELECT m.trainer.id, COUNT(m)
            FROM Member m
            WHERE m.trainer.id IN :trainerUserIds
            GROUP BY m.trainer.id
            """)
    List<Object[]> countAssignedMembersByTrainerUserIds(@Param("trainerUserIds") Collection<Long> trainerUserIds);
}
