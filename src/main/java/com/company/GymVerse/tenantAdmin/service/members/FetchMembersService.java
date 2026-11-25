package com.company.GymVerse.tenantAdmin.service.members;

import com.company.GymVerse.attendance.dto.MemberAttendanceSnapshot;
import com.company.GymVerse.attendance.service.AttendanceQueryPort;
import com.company.GymVerse.models.tenant.Member;
import com.company.GymVerse.models.tenant.MemberMembership;
import com.company.GymVerse.repositories.tenant.MemberMembershipRepository;
import com.company.GymVerse.repositories.tenant.MemberRepository;
import com.company.GymVerse.tenantAdmin.factory.TenantAdminOperation;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapper;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.MemberFilterStatus;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.members.FetchMembersRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.members.MemberListItemResponse;
import com.company.GymVerse.tenantAdmin.specifications.members.MemberSpecification;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates paginated, sorted, and JPA Specification-filtered retrieval of gym members.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FetchMembersService implements TenantAdminOperation {

    private final MemberRepository memberRepository;
    private final MemberMembershipRepository memberMembershipRepository;
    private final AttendanceQueryPort attendanceQueryPort;
    private final TenantAdminValidations validations;
    private final TenantAdminMapper mapper;

    @Override
    public TenantAdminRequestType getRequestType() {
        return TenantAdminRequestType.FETCH_MEMBERS;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public TenantAdminResponse execute(final TenantAdminRequest request) {
        log.info("Processing FETCH_MEMBERS operation");

        validations.validateTenantAdminContext();
        validations.validateRequest(request);

        FetchMembersRequest filter = request.getFetchMembersRequest();
        Pageable pageable = validations.validateFetchMembers(filter);
        MemberFilterStatus statusFilter = (filter != null && filter.getStatusFilter() != null)
                ? filter.getStatusFilter()
                : MemberFilterStatus.ALL;
        String search = filter != null ? filter.getSearch() : null;

        LocalDate today = LocalDate.now();
        List<Long> liveMemberIds = Collections.emptyList();
        if (statusFilter == MemberFilterStatus.LIVE) {
            // TODO [ATTENDANCE-MODULE]: Supply active checked-in member IDs from live attendance tracking.
            liveMemberIds = attendanceQueryPort.getLiveMemberIds();
        }

        Specification<Member> spec = MemberSpecification.withFilters(statusFilter, search, today, liveMemberIds);
        Page<Member> memberPage = memberRepository.findAll(spec, pageable);

        Map<Long, MemberMembership> primaryMembershipByMemberId = resolvePrimaryMemberships(memberPage.getContent());

        List<MemberListItemResponse> items = memberPage.getContent().stream()
                .map(member -> {
                    MemberMembership primaryMembership = primaryMembershipByMemberId.get(member.getId());
                    // TODO [ATTENDANCE-MODULE]: Replace per-member stub call with batched today check-in lookup.
                    MemberAttendanceSnapshot snapshot = attendanceQueryPort.getMemberAttendanceSnapshot(member.getId());
                    boolean checkedInToday = snapshot != null && snapshot.isCheckedInToday();
                    return mapper.toMemberListItem(member, primaryMembership, checkedInToday, today);
                })
                .toList();

        return mapper.toPagedMembersResponse("Members retrieved successfully", memberPage, items);
    }

    private Map<Long, MemberMembership> resolvePrimaryMemberships(final List<Member> members) {
        if (members == null || members.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> memberIds = members.stream().map(Member::getId).toList();
        List<MemberMembership> memberships = memberMembershipRepository.findAllWithPlanByMemberIdIn(memberIds);
        Map<Long, MemberMembership> result = new LinkedHashMap<>();
        for (MemberMembership membership : memberships) {
            Long memberId = membership.getMember() != null ? membership.getMember().getId() : null;
            if (memberId != null) {
                result.putIfAbsent(memberId, membership);
            }
        }
        return result;
    }
}
