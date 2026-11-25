package com.company.GymVerse.tenantAdmin.service.liveRoster;

import com.company.GymVerse.attendance.dto.LiveAttendanceSummary;
import com.company.GymVerse.attendance.dto.LiveRosterEntry;
import com.company.GymVerse.attendance.service.AttendanceQueryPort;
import com.company.GymVerse.tenantAdmin.config.TenantOperationsProperties;
import com.company.GymVerse.tenantAdmin.factory.TenantAdminOperation;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapper;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.liveRoster.FetchLiveRosterRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Orchestrates the Live Roster page gateway, delegating live checked-in member queries
 * and summary counters to the reusable AttendanceQueryPort.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FetchLiveRosterService implements TenantAdminOperation {

    private final AttendanceQueryPort attendanceQueryPort;
    private final TenantOperationsProperties tenantOperationsProperties;
    private final TenantAdminValidations validations;
    private final TenantAdminMapper mapper;

    @Override
    public TenantAdminRequestType getRequestType() {
        return TenantAdminRequestType.FETCH_LIVE_ROSTER;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public TenantAdminResponse execute(final TenantAdminRequest request) {
        log.info("Processing FETCH_LIVE_ROSTER operation");

        validations.validateTenantAdminContext();
        validations.validateRequest(request);

        FetchLiveRosterRequest filter = request.getFetchLiveRosterRequest();
        Pageable pageable = validations.validateFetchLiveRoster(filter);
        String membershipFilter = validations.parseLiveRosterMembershipFilter(
                filter != null ? filter.getMembershipFilter() : null
        );
        String search = filter != null ? filter.getSearch() : null;

        LocalDate expiringCutoff = LocalDate.now().plusDays(tenantOperationsProperties.getExpiringSoonDays());

        // TODO [ATTENDANCE-MODULE]: Query real live occupancy counts & checked-in member roster from Attendance module.
        LiveAttendanceSummary summary = attendanceQueryPort.getLiveAttendanceSummary(expiringCutoff);
        Page<LiveRosterEntry> rosterPage = attendanceQueryPort.getLiveRoster(search, membershipFilter, pageable);

        return mapper.toLiveRosterAdminResponse("Live roster retrieved successfully", summary, rosterPage);
    }
}
