package com.company.GymVerse.tenantAdmin.service.members;

import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.exceptions.common.ConflictException;
import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.enums.TenantRole;
import com.company.GymVerse.models.tenant.JoinRequest;
import com.company.GymVerse.models.tenant.Member;
import com.company.GymVerse.models.tenant.MemberMembership;
import com.company.GymVerse.models.tenant.MembershipPlan;
import com.company.GymVerse.models.tenant.Revenue;
import com.company.GymVerse.models.tenant.TenantUser;
import com.company.GymVerse.models.tenant.enums.JoinRequestStatus;
import com.company.GymVerse.models.tenant.enums.MemberType;
import com.company.GymVerse.models.tenant.enums.RevenueType;
import com.company.GymVerse.repositories.tenant.JoinRequestRepository;
import com.company.GymVerse.repositories.tenant.MemberMembershipRepository;
import com.company.GymVerse.repositories.tenant.MemberRepository;
import com.company.GymVerse.repositories.tenant.MembershipPlanRepository;
import com.company.GymVerse.repositories.tenant.RevenueRepository;
import com.company.GymVerse.repositories.tenant.TenantUserRepository;
import com.company.GymVerse.tenantAdmin.factory.TenantAdminOperation;
import com.company.GymVerse.tenantAdmin.helper.JoinRequestHelper;
import com.company.GymVerse.tenantAdmin.helper.TenantAdminContextHelper;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapper;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.members.ApproveJoinRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.members.ApproveJoinRequestResponse;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Orchestrates join request approval across Tenant DB (join_requests, users, members,
 * member_memberships, revenue) and Central DB (user_tenant_roles) with compensation rollback.
 */
@Slf4j
@Service
public class ApproveJoinRequestService implements TenantAdminOperation {

    private final JoinRequestRepository joinRequestRepository;
    private final MembershipPlanRepository membershipPlanRepository;
    private final TenantUserRepository tenantUserRepository;
    private final MemberRepository memberRepository;
    private final MemberMembershipRepository memberMembershipRepository;
    private final RevenueRepository revenueRepository;
    private final TenantAdminValidations validations;
    private final TenantAdminContextHelper contextHelper;
    private final JoinRequestHelper joinRequestHelper;
    private final TenantAdminMapper mapper;
    private final PlatformTransactionManager tenantTransactionManager;

    public ApproveJoinRequestService(
            final JoinRequestRepository joinRequestRepository,
            final MembershipPlanRepository membershipPlanRepository,
            final TenantUserRepository tenantUserRepository,
            final MemberRepository memberRepository,
            final MemberMembershipRepository memberMembershipRepository,
            final RevenueRepository revenueRepository,
            final TenantAdminValidations validations,
            final TenantAdminContextHelper contextHelper,
            final JoinRequestHelper joinRequestHelper,
            final TenantAdminMapper mapper,
            @Qualifier("tenantTransactionManager") final PlatformTransactionManager tenantTransactionManager
    ) {
        this.joinRequestRepository = joinRequestRepository;
        this.membershipPlanRepository = membershipPlanRepository;
        this.tenantUserRepository = tenantUserRepository;
        this.memberRepository = memberRepository;
        this.memberMembershipRepository = memberMembershipRepository;
        this.revenueRepository = revenueRepository;
        this.validations = validations;
        this.contextHelper = contextHelper;
        this.joinRequestHelper = joinRequestHelper;
        this.mapper = mapper;
        this.tenantTransactionManager = tenantTransactionManager;
    }

    @Override
    public TenantAdminRequestType getRequestType() {
        return TenantAdminRequestType.APPROVE_JOIN_REQUEST;
    }

    @Override
    public TenantAdminResponse execute(final TenantAdminRequest request) {
        log.info("Processing APPROVE_JOIN_REQUEST operation");

        validations.validateTenantAdminContext();
        validations.validateRequest(request);

        ApproveJoinRequest payload = request.getApproveJoinRequest();
        validations.validateApproveJoinRequest(payload);

        Long tenantId = contextHelper.extractTenantId();

        // 1. Validate tenant pre-conditions & gather counts in read-only tenant TX
        TenantPreCheckResult preCheck = executeTenantPreCheck(payload);

        // 2. Validate central quota, subscription health & applicant central identity
        JoinRequestHelper.CentralApprovalContext centralContext = joinRequestHelper.validateAndResolveCentralContext(
                tenantId,
                preCheck.centralUserId(),
                preCheck.currentMemberCount()
        );

        // 3. Persist tenant approval records (JoinRequest, TenantUser, Member, MemberMembership, Revenue)
        TenantApprovalArtifacts artifacts = executeTenantApprovalWrite(payload, centralContext.userAccount());

        // 4. Persist central UserTenantRole(MEMBER, ACTIVE) with safe tenant compensation on failure
        try {
            joinRequestHelper.persistCentralMemberRole(centralContext);
        } catch (Exception ex) {
            log.error("Central role provisioning failed after tenant approval for joinRequestId={}: {}",
                    payload.getJoinRequestId(), ex.getMessage(), ex);
            compensateTenantApproval(artifacts);
            throw ex;
        }

        ApproveJoinRequestResponse responsePayload = mapper.toApproveJoinRequestResponse(
                artifacts.joinRequest(),
                artifacts.member(),
                artifacts.membership(),
                payload.getPaymentMode()
        );

        log.info("Approved joinRequestId={} -> memberId={} membershipId={}",
                artifacts.joinRequest().getId(), artifacts.member().getId(), artifacts.membership().getId());

        return mapper.toApproveJoinRequestAdminResponse("Join request approved and member onboarded successfully", responsePayload);
    }

    private record TenantPreCheckResult(Long centralUserId, long currentMemberCount) {
    }

    private record TenantApprovalArtifacts(
            JoinRequest joinRequest,
            TenantUser memberUser,
            Member member,
            MemberMembership membership,
            Revenue revenue
    ) {
    }

    private TenantPreCheckResult executeTenantPreCheck(final ApproveJoinRequest payload) {
        TransactionTemplate readTx = new TransactionTemplate(tenantTransactionManager);
        readTx.setReadOnly(true);

        return readTx.execute(status -> {
            JoinRequest joinRequest = joinRequestRepository.findById(payload.getJoinRequestId())
                    .orElseThrow(() -> new ResourceNotFoundException("Join request not found with ID: " + payload.getJoinRequestId()));

            if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {
                throw new BadRequestException("Only PENDING join requests can be approved; current status is " + joinRequest.getStatus());
            }

            MembershipPlan plan = membershipPlanRepository.findById(payload.getPlanId())
                    .orElseThrow(() -> new ResourceNotFoundException("Membership plan not found with ID: " + payload.getPlanId()));
            if (!plan.isActive()) {
                throw new BadRequestException("Cannot assign inactive membership plan ID: " + plan.getId());
            }

            validations.validateJoinRequestPayment(payload.getPaymentStatus(), payload.getPaidAmount(), plan);

            if (tenantUserRepository.existsByCentralUserId(joinRequest.getCentralUserId())
                    || tenantUserRepository.existsByEmail(joinRequest.getEmail())) {
                throw new ConflictException("Applicant already has a user profile in this gym");
            }

            if (payload.getTrainerId() != null) {
                TenantUser trainer = tenantUserRepository.findById(payload.getTrainerId())
                        .orElseThrow(() -> new ResourceNotFoundException("Trainer not found with user ID: " + payload.getTrainerId()));
                if (trainer.getRole() != TenantRole.TRAINER) {
                    throw new BadRequestException("Specified user ID " + payload.getTrainerId() + " does not hold TRAINER role");
                }
            }

            return new TenantPreCheckResult(joinRequest.getCentralUserId(), memberRepository.count());
        });
    }

    private TenantApprovalArtifacts executeTenantApprovalWrite(
            final ApproveJoinRequest payload,
            final UserAccount centralUser
    ) {
        TransactionTemplate writeTx = new TransactionTemplate(tenantTransactionManager);

        return writeTx.execute(status -> {
            JoinRequest joinRequest = joinRequestRepository.findById(payload.getJoinRequestId())
                    .orElseThrow(() -> new ResourceNotFoundException("Join request not found with ID: " + payload.getJoinRequestId()));

            if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {
                throw new BadRequestException("Join request is no longer PENDING");
            }

            MembershipPlan plan = membershipPlanRepository.findById(payload.getPlanId())
                    .orElseThrow(() -> new ResourceNotFoundException("Membership plan not found with ID: " + payload.getPlanId()));

            TenantUser reviewingAdmin = contextHelper.resolveCurrentAdminUser();
            TenantUser trainer = payload.getTrainerId() != null
                    ? tenantUserRepository.findById(payload.getTrainerId()).orElse(null)
                    : null;

            joinRequest.setStatus(JoinRequestStatus.APPROVED);
            joinRequest.setAdminNotes(payload.getAdminNotes());
            joinRequest.setReviewedAt(Instant.now());
            joinRequest.setReviewedByAdmin(reviewingAdmin);
            JoinRequest savedJoinRequest = joinRequestRepository.save(joinRequest);

            String firstName = firstNonBlank(joinRequest.getFirstName(), centralUser.getFirstName());
            String lastName = firstNonBlank(joinRequest.getLastName(), centralUser.getLastName());
            String phone = joinRequest.getPhoneNumber() != null ? joinRequest.getPhoneNumber() : centralUser.getPhoneNumber();

            TenantUser memberUser = tenantUserRepository.save(TenantUser.builder()
                    .centralUserId(joinRequest.getCentralUserId())
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(joinRequest.getEmail())
                    .phoneNumber(phone)
                    .role(TenantRole.MEMBER)
                    .active(true)
                    .build());

            LocalDate today = LocalDate.now();
            LocalDate endDate = today.plusDays(plan.getDurationDays());
            boolean isTrial = plan.isTrial();

            Member member = memberRepository.save(Member.builder()
                    .user(memberUser)
                    .trainer(trainer)
                    .independent(Boolean.TRUE.equals(payload.getIndependent()))
                    .joinDate(today)
                    .gymId(joinRequestHelper.generateUniqueMemberGymId())
                    .type(isTrial ? MemberType.TRIAL : MemberType.REGULAR)
                    .trialEndDate(isTrial ? endDate : null)
                    .build());

            MemberMembership membership = memberMembershipRepository.save(MemberMembership.builder()
                    .member(member)
                    .plan(plan)
                    .priceAtPurchase(plan.getBasePrice())
                    .discountType(plan.getDiscountType())
                    .discountValue(plan.getDiscountValue())
                    .finalPrice(plan.getFinalPrice())
                    .paidAmount(payload.getPaidAmount())
                    .paymentStatus(payload.getPaymentStatus())
                    .startDate(today)
                    .endDate(endDate)
                    .active(true)
                    .renewal(false)
                    .trial(isTrial)
                    .build());

            Revenue revenue = null;
            if (payload.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
                revenue = revenueRepository.save(Revenue.builder()
                        .memberMembership(membership)
                        .amount(payload.getPaidAmount())
                        .paymentMode(payload.getPaymentMode())
                        .revenueType(RevenueType.NEW_MEMBERSHIP)
                        .transactionReference(payload.getTransactionReference())
                        .notes(payload.getAdminNotes())
                        .receivedAt(Instant.now())
                        .recordedByAdmin(reviewingAdmin)
                        .build());
            }

            return new TenantApprovalArtifacts(savedJoinRequest, memberUser, member, membership, revenue);
        });
    }

    private void compensateTenantApproval(final TenantApprovalArtifacts artifacts) {
        try {
            TransactionTemplate writeTx = new TransactionTemplate(tenantTransactionManager);
            writeTx.executeWithoutResult(status -> {
                if (artifacts.revenue() != null && artifacts.revenue().getId() != null) {
                    revenueRepository.deleteById(artifacts.revenue().getId());
                }
                if (artifacts.membership() != null && artifacts.membership().getId() != null) {
                    memberMembershipRepository.deleteById(artifacts.membership().getId());
                }
                if (artifacts.member() != null && artifacts.member().getId() != null) {
                    memberRepository.deleteById(artifacts.member().getId());
                }
                if (artifacts.memberUser() != null && artifacts.memberUser().getId() != null) {
                    tenantUserRepository.deleteById(artifacts.memberUser().getId());
                }
                if (artifacts.joinRequest() != null && artifacts.joinRequest().getId() != null) {
                    joinRequestRepository.findById(artifacts.joinRequest().getId()).ifPresent(jr -> {
                        jr.setStatus(JoinRequestStatus.PENDING);
                        jr.setReviewedAt(null);
                        jr.setReviewedByAdmin(null);
                        jr.setAdminNotes(null);
                        joinRequestRepository.save(jr);
                    });
                }
            });
        } catch (Exception compEx) {
            log.error("Compensation rollback failed for joinRequestId={}: {}",
                    artifacts.joinRequest().getId(), compEx.getMessage(), compEx);
        }
    }

    private String firstNonBlank(final String primary, final String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        return fallback != null ? fallback.trim() : "";
    }
}
