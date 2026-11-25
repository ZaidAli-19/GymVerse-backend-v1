package com.company.GymVerse.tenantAdmin.response.trainers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Trainer address book row returned in paginated FETCH_TRAINERS responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerItemResponse {

    private Long trainerId;
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String specialization;
    private Integer experienceYears;
    private String bio;
    private LocalDate joinedDate;
    private boolean active;
    private long assignedMembersCount;
}
