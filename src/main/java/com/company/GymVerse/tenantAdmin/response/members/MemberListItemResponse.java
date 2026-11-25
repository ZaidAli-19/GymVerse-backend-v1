package com.company.GymVerse.tenantAdmin.response.members;

import com.company.GymVerse.models.tenant.enums.PaymentStatus;
import com.company.GymVerse.models.tenant.enums.PlanTheme;
import com.company.GymVerse.tenantAdmin.response.enums.MemberMembershipStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Member directory row returned in paginated FETCH_MEMBERS responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberListItemResponse {

    private Long memberId;
    private String gymId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String planName;
    private String tier;
    private PlanTheme themeKey;
    private MemberMembershipStatus membershipStatus;
    private long daysRemaining;
    private BigDecimal paidAmount;
    private BigDecimal finalPrice;
    private PaymentStatus paymentStatus;
    private boolean checkedInToday;
}
