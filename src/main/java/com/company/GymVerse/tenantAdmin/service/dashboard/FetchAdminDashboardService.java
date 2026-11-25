package com.company.GymVerse.tenantAdmin.service.dashboard;

import com.company.GymVerse.attendance.dto.LiveAttendanceSummary;
import com.company.GymVerse.attendance.service.AttendanceQueryPort;
import com.company.GymVerse.models.tenant.PlanTier;
import com.company.GymVerse.models.tenant.enums.RevenueType;
import com.company.GymVerse.repositories.tenant.MemberMembershipRepository;
import com.company.GymVerse.repositories.tenant.MemberRepository;
import com.company.GymVerse.repositories.tenant.PlanTierRepository;
import com.company.GymVerse.repositories.tenant.RevenueRepository;
import com.company.GymVerse.tenantAdmin.config.TenantOperationsProperties;
import com.company.GymVerse.tenantAdmin.factory.TenantAdminOperation;
import com.company.GymVerse.tenantAdmin.helper.MembershipCalculationHelper;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapper;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.dashboard.FetchAdminDashboardRequest;
import com.company.GymVerse.tenantAdmin.request.enums.MemberFilterStatus;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.dashboard.AdminDashboardResponse;
import com.company.GymVerse.tenantAdmin.response.dashboard.DashboardExpiryCardResponse;
import com.company.GymVerse.tenantAdmin.response.dashboard.DashboardRevenueCardResponse;
import com.company.GymVerse.tenantAdmin.response.dashboard.DashboardTierBreakdownItem;
import com.company.GymVerse.tenantAdmin.specifications.members.MemberSpecification;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the executive Admin Dashboard snapshot: top KPIs, tier breakdowns,
 * revenue card, expiring-soon card, and stubbed live attendance metrics via AttendanceQueryPort.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FetchAdminDashboardService implements TenantAdminOperation {

    private final MemberRepository memberRepository;
    private final MemberMembershipRepository memberMembershipRepository;
    private final PlanTierRepository planTierRepository;
    private final RevenueRepository revenueRepository;
    private final AttendanceQueryPort attendanceQueryPort;
    private final TenantOperationsProperties tenantOperationsProperties;
    private final TenantAdminValidations validations;
    private final TenantAdminMapper mapper;

    @Override
    public TenantAdminRequestType getRequestType() {
        return TenantAdminRequestType.FETCH_ADMIN_DASHBOARD;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public TenantAdminResponse execute(final TenantAdminRequest request) {
        log.info("Processing FETCH_ADMIN_DASHBOARD operation");

        validations.validateTenantAdminContext();
        validations.validateRequest(request);

        FetchAdminDashboardRequest payload = request.getFetchAdminDashboardRequest();
        LocalDate today = (payload != null && payload.getReferenceDate() != null)
                ? payload.getReferenceDate()
                : LocalDate.now();
        int expiringWindowDays = tenantOperationsProperties.getExpiringSoonDays();
        LocalDate expiringCutoff = today.plusDays(expiringWindowDays);

        long totalMembers = memberRepository.count();
        long expiredMembers = memberRepository.count(
                MemberSpecification.withFilters(MemberFilterStatus.EXPIRED, null, today, Collections.emptyList())
        );
        long activeMembers = Math.max(0L, totalMembers - expiredMembers);
        BigDecimal activeRate = MembershipCalculationHelper.safePercentage(
                BigDecimal.valueOf(activeMembers),
                BigDecimal.valueOf(totalMembers)
        );

        // TODO [ATTENDANCE-MODULE]: Populate live members and live expiring-soon counts via AttendanceQueryPort.
        LiveAttendanceSummary liveSummary = attendanceQueryPort.getLiveAttendanceSummary(expiringCutoff);
        long liveMembers = liveSummary != null ? liveSummary.getLiveMembersCount() : 0L;
        long liveExpiringSoonCount = liveSummary != null ? liveSummary.getLiveExpiringSoonCount() : 0L;

        List<DashboardTierBreakdownItem> tierBreakdown = buildTierBreakdown(today);
        long expiringSoonCount = memberMembershipRepository.countExpiringSoonMembers(today, expiringCutoff);
        DashboardRevenueCardResponse revenueCard = buildRevenueCard();

        AdminDashboardResponse dashboard = AdminDashboardResponse.builder()
                .totalMembers(totalMembers)
                .liveMembers(liveMembers)
                .totalRevenue(revenueCard.getTotalRevenue())
                .activeRate(activeRate)
                .activeByTier(tierBreakdown)
                .membershipCard(AdminDashboardResponse.DashboardMembershipCard.builder()
                        .totalMembers(totalMembers)
                        .tierBreakdown(tierBreakdown)
                        .build())
                .revenueCard(revenueCard)
                .membershipExpiryCard(DashboardExpiryCardResponse.builder()
                        .expiringSoonCount(expiringSoonCount)
                        .expiringSoonWindowDays(expiringWindowDays)
                        .liveExpiringSoonCount(liveExpiringSoonCount)
                        .build())
                .build();

        return mapper.toAdminDashboardAdminResponse("Admin dashboard retrieved successfully", dashboard);
    }

    private List<DashboardTierBreakdownItem> buildTierBreakdown(final LocalDate today) {
        List<PlanTier> tiers = planTierRepository.findAllByOrderByDefaultTierDescNameAsc();
        List<Object[]> rows = memberMembershipRepository.countActiveMembersGroupedByTier(today);

        Map<String, Long> countByTierName = new LinkedHashMap<>();
        if (rows != null) {
            for (Object[] row : rows) {
                if (row != null && row.length >= 3 && row[0] instanceof String tierName && row[2] instanceof Number num) {
                    countByTierName.put(tierName.toUpperCase(), num.longValue());
                }
            }
        }

        return tiers.stream()
                .map(tier -> DashboardTierBreakdownItem.builder()
                        .tierName(tier.getName())
                        .themeKey(tier.getThemeKey())
                        .count(countByTierName.getOrDefault(tier.getName().toUpperCase(), 0L))
                        .build())
                .toList();
    }

    private DashboardRevenueCardResponse buildRevenueCard() {
        List<Object[]> rows = revenueRepository.sumAmountGroupedByRevenueType();
        Map<RevenueType, BigDecimal> sums = new EnumMap<>(RevenueType.class);
        if (rows != null) {
            for (Object[] row : rows) {
                if (row != null && row.length >= 2 && row[0] instanceof RevenueType type) {
                    sums.merge(type, toBigDecimal(row[1]), BigDecimal::add);
                }
            }
        }

        BigDecimal newMembership = scale(sums.getOrDefault(RevenueType.NEW_MEMBERSHIP, BigDecimal.ZERO));
        BigDecimal renewal = scale(sums.getOrDefault(RevenueType.RENEWAL, BigDecimal.ZERO));
        BigDecimal other = scale(sums.getOrDefault(RevenueType.OTHER, BigDecimal.ZERO));
        BigDecimal total = scale(newMembership.add(renewal).add(other));

        return DashboardRevenueCardResponse.builder()
                .totalRevenue(total)
                .newMembershipRevenue(newMembership)
                .renewalRevenue(renewal)
                .otherRevenue(other)
                .build();
    }

    private BigDecimal toBigDecimal(final Object raw) {
        if (raw == null) {
            return BigDecimal.ZERO;
        }
        if (raw instanceof BigDecimal bd) {
            return bd;
        }
        if (raw instanceof Number num) {
            return BigDecimal.valueOf(num.doubleValue());
        }
        return new BigDecimal(raw.toString());
    }

    private BigDecimal scale(final BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
