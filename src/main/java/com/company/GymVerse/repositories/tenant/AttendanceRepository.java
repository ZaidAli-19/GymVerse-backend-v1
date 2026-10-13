package com.company.GymVerse.repositories.tenant;

import com.company.GymVerse.models.tenant.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Attendance entities.
 */
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByMemberIdAndDate(Long memberId, LocalDate date);

    List<Attendance> findByDate(LocalDate date);
}
