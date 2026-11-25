package com.company.GymVerse.tenantAdmin.request;

import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.members.ApproveJoinRequest;
import com.company.GymVerse.tenantAdmin.request.members.FetchJoinRequestsRequest;
import com.company.GymVerse.tenantAdmin.request.members.FetchMemberDetailsRequest;
import com.company.GymVerse.tenantAdmin.request.members.FetchMembersRequest;
import com.company.GymVerse.tenantAdmin.request.members.RejectJoinRequest;
import com.company.GymVerse.tenantAdmin.request.plans.CreatePlanRequest;
import com.company.GymVerse.tenantAdmin.request.plans.FetchPlansRequest;
import com.company.GymVerse.tenantAdmin.request.plans.UpdatePlanAvailabilityRequest;
import com.company.GymVerse.tenantAdmin.request.subscription.FetchTenantSubscriptionRequest;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified request envelope for all tenant-scoped administrative operations on /api/tenant-admin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantAdminRequest {

    @JsonAlias({"tenantAdminRequestType", "requestType"})
    private TenantAdminRequestType tenantAdminRequestType;

    @JsonAlias({"fetchTenantSubscriptionRequest", "fetchTenantSubscription"})
    private FetchTenantSubscriptionRequest fetchTenantSubscriptionRequest;

    @JsonAlias({"fetchPlansRequest", "fetchPlans"})
    private FetchPlansRequest fetchPlansRequest;

    @JsonAlias({"createPlanRequest", "createPlan"})
    private CreatePlanRequest createPlanRequest;

    @JsonAlias({"updatePlanAvailabilityRequest", "updatePlanAvailability"})
    private UpdatePlanAvailabilityRequest updatePlanAvailabilityRequest;

    @JsonAlias({"fetchJoinRequestsRequest", "fetchJoinRequests"})
    private FetchJoinRequestsRequest fetchJoinRequestsRequest;

    @JsonAlias({"approveJoinRequest", "approveJoin"})
    private ApproveJoinRequest approveJoinRequest;

    @JsonAlias({"rejectJoinRequest", "rejectJoin"})
    private RejectJoinRequest rejectJoinRequest;

    @JsonAlias({"fetchMembersRequest", "fetchMembers"})
    private FetchMembersRequest fetchMembersRequest;

    @JsonAlias({"fetchMemberDetailsRequest", "fetchMemberDetails"})
    private FetchMemberDetailsRequest fetchMemberDetailsRequest;
}
