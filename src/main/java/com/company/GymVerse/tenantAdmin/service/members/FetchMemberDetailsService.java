package com.company.GymVerse.tenantAdmin.service.members;

import com.company.GymVerse.attendance.dto.MemberAttendanceSnapshot;
import com.company.GymVerse.attendance.service.AttendanceQueryPort;
import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.models.tenant.Member;
import com.company.GymVerse.models.tenant.MemberMembership;
import com.company.GymVerse.repositories.tenant.MemberMembershipRepository;
import com.company.GymVerse.repositories.tenant.MemberRepository;
import com.company.GymVerse.repositories.tenant.RevenueRepository;
import com.company.GymVerse.tenantAdmin.factory.TenantAdminOperation;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapper;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.members.FetchMemberDetailsRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.members.MemberDetailsResponse;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Orchestrates full Member Details view assembly across attendance, quick stats, membership, and contact cards.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FetchMemberDetailsService implements TenantAdminOperation {

    private final MemberRepository memberRepository;
    private final MemberMembershipRepository memberMembershipRepository;
    private final RevenueRepository revenueRepository;
    private final AttendanceQueryPort attendanceQueryPort;
    private final TenantAdminValidations validations;
    private final TenantAdminMapper mapper;

    @Override
    public TenantAdminRequestType getRequestType() {
        return TenantAdminRequestType.FETCH_MEMBER_DETAILS;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public TenantAdminResponse execute(final TenantAdminRequest request) {
        log.info("Processing FETCH_MEMBER_DETAILS operation");

        validations.validateTenantAdminContext();
        validations.validateRequest(request);

        FetchMemberDetailsRequest payload = request.getFetchMemberDetailsRequest();
        validations.validateFetchMemberDetails(payload);

        Long memberId = payload.getMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with ID: " + memberId));

        List<MemberMembership> memberships = memberMembershipRepository.findAllWithPlanAndFacilitiesByMemberId(memberId);
        MemberMembership primaryMembership = memberships.isEmpty() ? null : memberships.get(0);

        BigDecimal totalAmountPaid = revenueRepository.sumTotalPaidByMemberId(memberId);

        // TODO [ATTENDANCE-MODULE]: Populate real lifetime visits, streak, and today's check-in elapsed time.
        MemberAttendanceSnapshot attendanceSnapshot = attendanceQueryPort.getMemberAttendanceSnapshot(memberId);

        MemberDetailsResponse details = mapper.toMemberDetails(
                member,
                primaryMembership,
                totalAmountPaid,
                attendanceSnapshot,
                LocalDate.now()
        );

        return mapper.toMemberDetailsAdminResponse("Member details retrieved successfully", details);
    }
}
