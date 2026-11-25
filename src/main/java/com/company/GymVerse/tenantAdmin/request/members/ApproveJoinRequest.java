package com.company.GymVerse.tenantAdmin.request.members;

import com.company.GymVerse.models.tenant.enums.PaymentMode;
import com.company.GymVerse.models.tenant.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request payload for approving a pending join request and provisioning membership + revenue.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApproveJoinRequest {

    private Long joinRequestId;
    private Long planId;
    private PaymentStatus paymentStatus;
    private BigDecimal paidAmount;
    private PaymentMode paymentMode;
    private String transactionReference;
    private String adminNotes;
    private Long trainerId;
    private Boolean independent;
}
