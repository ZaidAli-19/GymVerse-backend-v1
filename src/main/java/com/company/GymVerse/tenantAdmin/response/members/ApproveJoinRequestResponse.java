package com.company.GymVerse.tenantAdmin.response.members;

import com.company.GymVerse.models.tenant.enums.JoinRequestStatus;
import com.company.GymVerse.models.tenant.enums.PaymentMode;
import com.company.GymVerse.models.tenant.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Confirmation payload returned after approving a join request and creating membership + revenue records.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveJoinRequestResponse {

    private Long joinRequestId;
    private Long memberId;
    private Long membershipId;
    private String gymId;
    private JoinRequestStatus status;
    private PaymentStatus paymentStatus;
    private PaymentMode paymentMode;
    private BigDecimal paidAmount;
    private BigDecimal finalPrice;
    private BigDecimal remainingDue;
    private LocalDate startDate;
    private LocalDate endDate;
}
