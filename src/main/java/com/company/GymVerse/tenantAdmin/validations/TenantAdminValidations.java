package com.company.GymVerse.tenantAdmin.validations;

import com.company.GymVerse.models.tenant.MembershipPlan;
import com.company.GymVerse.models.tenant.enums.ExpenseCategory;
import com.company.GymVerse.models.tenant.enums.JoinRequestStatus;
import com.company.GymVerse.models.tenant.enums.PaymentStatus;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.finance.CreateExpenseRequest;
import com.company.GymVerse.tenantAdmin.request.finance.FetchExpenseTransactionsRequest;
import com.company.GymVerse.tenantAdmin.request.members.ApproveJoinRequest;
import com.company.GymVerse.tenantAdmin.request.members.FetchJoinRequestsRequest;
import com.company.GymVerse.tenantAdmin.request.members.FetchMemberDetailsRequest;
import com.company.GymVerse.tenantAdmin.request.members.FetchMembersRequest;
import com.company.GymVerse.tenantAdmin.request.members.RejectJoinRequest;
import com.company.GymVerse.tenantAdmin.request.plans.CreatePlanRequest;
import com.company.GymVerse.tenantAdmin.request.plans.FetchPlansRequest;
import com.company.GymVerse.tenantAdmin.request.plans.UpdatePlanAvailabilityRequest;
import com.company.GymVerse.tenantAdmin.request.trainers.FetchTrainersRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

import java.util.Set;

/**
 * Validation interface defining security context and constraint rules for tenant administration flows.
 */
public interface TenantAdminValidations {

    /**
     * Validates that the active request context is an authenticated tenant administrator session,
     * enforcing that TenantContext is bound, token is TENANT_ACCOUNT, and caller holds ROLE_TENANT_ADMIN.
     */
    void validateTenantAdminContext();

    /**
     * Validates domain invariants and non-null constraints for incoming TenantAdminRequest envelopes.
     *
     * @param request the tenant admin request envelope
     */
    void validateRequest(TenantAdminRequest request);

    /**
     * Validates pagination parameters and whitelist-guarded sorting, returning a constructed Pageable.
     */
    Pageable validateAndBuildPageable(
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection,
            String defaultSortBy,
            Set<String> allowedSortFields
    );

    /**
     * Validates pagination and sorting fields for FETCH_PLANS queries.
     */
    Pageable validateFetchPlans(FetchPlansRequest request);

    /**
     * Validates CreatePlanRequest fields, tier/theme constraints, facilities, pricing, and uniqueness.
     */
    void validateCreatePlan(CreatePlanRequest request);

    /**
     * Validates UpdatePlanAvailabilityRequest required fields.
     */
    void validateUpdatePlanAvailability(UpdatePlanAvailabilityRequest request);

    /**
     * Validates pagination and sorting fields for FETCH_JOIN_REQUESTS queries.
     */
    Pageable validateFetchJoinRequests(FetchJoinRequestsRequest request);

    /**
     * Parses optional join request status filter ("ALL" or enum value).
     */
    JoinRequestStatus parseJoinRequestStatusFilter(String status);

    /**
     * Validates ApproveJoinRequest structural payload constraints.
     */
    void validateApproveJoinRequest(ApproveJoinRequest request);

    /**
     * Validates payment amount invariants against plan pricing and PAID / PARTIALLY_PAID status.
     */
    void validateJoinRequestPayment(PaymentStatus paymentStatus, BigDecimal paidAmount, MembershipPlan plan);

    /**
     * Validates RejectJoinRequest structural payload constraints.
     */
    void validateRejectJoinRequest(RejectJoinRequest request);

    /**
     * Validates pagination and whitelisted sorting fields for FETCH_MEMBERS queries.
     */
    Pageable validateFetchMembers(FetchMembersRequest request);

    /**
     * Validates FetchMemberDetailsRequest required positive memberId.
     */
    void validateFetchMemberDetails(FetchMemberDetailsRequest request);

    /**
     * Validates pagination and whitelisted sorting fields for FETCH_TRAINERS queries.
     */
    Pageable validateFetchTrainers(FetchTrainersRequest request);

    /**
     * Validates date range, pagination, and whitelisted sorting fields for FETCH_EXPENSE_TRANSACTIONS queries.
     */
    Pageable validateFetchExpenseTransactions(FetchExpenseTransactionsRequest request);

    /**
     * Parses optional expense category filter ("ALL" or ExpenseCategory value).
     */
    ExpenseCategory parseExpenseCategoryFilter(String category);

    /**
     * Validates CreateExpenseRequest fields and resolves standard or custom OTHER ExpenseCategory.
     */
    ExpenseCategory validateAndResolveCreateExpense(CreateExpenseRequest request);
}
