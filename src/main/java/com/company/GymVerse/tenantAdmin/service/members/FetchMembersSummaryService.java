package com.company.GymVerse.tenantAdmin.service.members;

import com.company.GymVerse.attendance.dto.LiveAttendanceSummary;
import com.company.GymVerse.attendance.service.AttendanceQueryPort;
import com.company.GymVerse.repositories.tenant.MemberRepository;
import com.company.GymVerse.tenantAdmin.config.TenantOperationsProperties;
import com.company.GymVerse.tenantAdmin.factory.TenantAdminOperation;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapper;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.MemberFilterStatus;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.specifications.members.MemberSpecification;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;

/**
 * Orchestrates Members mini-dashboard counts: total, live, expired, and dual-semantics trial counts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FetchMembersSummaryService implements TenantAdminOperation {

    private final MemberRepository memberRepository;
    private final AttendanceQueryPort attendanceQueryPort;
    private final TenantOperationsProperties tenantOperationsProperties;
    private final TenantAdminValidations validations;
    private final TenantAdminMapper mapper;

    @Override
    public TenantAdminRequestType getRequestType() {
        return TenantAdminRequestType.FETCH_MEMBERS_SUMMARY;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public TenantAdminResponse execute(final TenantAdminRequest request) {
        log.info("Processing FETCH_MEMBERS_SUMMARY operation");

        validations.validateTenantAdminContext();
        validations.validateRequest(request);

        LocalDate today = LocalDate.now();
        LocalDate expiringSoonCutoff = today.plusDays(tenantOperationsProperties.getExpiringSoonDays());

        long totalMembersCount = memberRepository.count();
        long expiredCount = memberRepository.count(
                MemberSpecification.withFilters(MemberFilterStatus.EXPIRED, null, today, Collections.emptyList())
        );
        long trialCount = memberRepository.count(
                MemberSpecification.withFilters(MemberFilterStatus.TRIAL, null, today, Collections.emptyList())
        );

        // TODO [ATTENDANCE-MODULE]: Live member count delegates to AttendanceQueryPort; stub returns 0L.
        LiveAttendanceSummary liveSummary = attendanceQueryPort.getLiveAttendanceSummary(expiringSoonCutoff);
        long liveCount = liveSummary != null ? liveSummary.getLiveMembersCount() : 0L;

        return mapper.toMembersSummaryResponse(
                "Members summary retrieved successfully",
                totalMembersCount,
                liveCount,
                expiredCount,
                trialCount
        );
    }
}
