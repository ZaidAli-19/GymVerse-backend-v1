package com.company.GymVerse.tenantAdmin.response.members;

import com.company.GymVerse.models.tenant.enums.PaymentStatus;
import com.company.GymVerse.models.tenant.enums.PlanTheme;
import com.company.GymVerse.tenantAdmin.response.enums.MemberMembershipStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Comprehensive member profile payload structured around the 4 Member Details UI cards.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDetailsResponse {

    private AttendanceActivityCard attendanceActivity;
    private QuickStatsCard quickStats;
    private MembershipDetailsCard membershipDetails;
    private ContactDetailsCard contactDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceActivityCard {
        private long totalVisitCount;
        private int currentStreak;
        private DailyAttendanceCard dailyAttendance;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyAttendanceCard {
        private boolean checkedIn;
        private Instant checkInTime;
        private Long elapsedMinutes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickStatsCard {
        private BigDecimal totalAmountPaid;
        private long daysLeftUntilExpiry;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MembershipDetailsCard {
        private String planName;
        private String tier;
        private PlanTheme themeKey;
        private List<String> facilities;
        private LocalDate joinDate;
        private LocalDate startDate;
        private LocalDate expiryDate;
        private long remainingDays;
        private BigDecimal finalPrice;
        private BigDecimal paidAmount;
        private PaymentStatus paymentStatus;

        @JsonProperty("isTrial")
        private boolean trial;

        private MemberMembershipStatus membershipStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactDetailsCard {
        private Long memberId;
        private String gymId;
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
    }
}
