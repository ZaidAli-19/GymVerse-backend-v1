package com.company.GymVerse.tenantAdmin.specifications.members;

import com.company.GymVerse.models.tenant.Member;
import com.company.GymVerse.models.tenant.MemberMembership;
import com.company.GymVerse.models.tenant.MembershipPlan;
import com.company.GymVerse.models.tenant.TenantUser;
import com.company.GymVerse.tenantAdmin.request.enums.MemberFilterStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic Spring Data JPA Specification builder for querying the tenant member directory.
 */
public final class MemberSpecification {

    private MemberSpecification() {
    }

    /**
     * Composes dynamic filters for directory tab status (ALL, LIVE, EXPIRED, TRIAL)
     * and case-insensitive partial search across member identity fields and gymId.
     */
    public static Specification<Member> withFilters(
            final MemberFilterStatus statusFilter,
            final String search,
            final LocalDate today,
            final List<Long> liveMemberIds
    ) {
        LocalDate effectiveToday = today != null ? today : LocalDate.now();
        MemberFilterStatus effectiveStatus = statusFilter != null ? statusFilter : MemberFilterStatus.ALL;

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (effectiveStatus == MemberFilterStatus.LIVE) {
                if (liveMemberIds == null || liveMemberIds.isEmpty()) {
                    // TODO [ATTENDANCE-MODULE]: When live module arrives, supply active checked-in member IDs.
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("id").in(liveMemberIds));
                }
            } else if (effectiveStatus == MemberFilterStatus.TRIAL && query != null) {
                Subquery<Long> trialSubquery = query.subquery(Long.class);
                Root<MemberMembership> mm = trialSubquery.from(MemberMembership.class);
                Join<MemberMembership, MembershipPlan> planJoin = mm.join("plan", JoinType.LEFT);
                trialSubquery.select(cb.literal(1L))
                        .where(
                                cb.equal(mm.get("member"), root),
                                cb.isTrue(mm.get("active")),
                                cb.greaterThanOrEqualTo(mm.get("endDate"), effectiveToday),
                                cb.or(
                                        cb.isTrue(mm.get("trial")),
                                        cb.isTrue(planJoin.get("trial"))
                                )
                        );

                Predicate trialDateActive = cb.and(
                        cb.isNotNull(root.get("trialEndDate")),
                        cb.greaterThanOrEqualTo(root.get("trialEndDate"), effectiveToday)
                );
                predicates.add(cb.or(trialDateActive, cb.exists(trialSubquery)));
            } else if (effectiveStatus == MemberFilterStatus.EXPIRED && query != null) {
                Subquery<Long> activeMembershipSubquery = query.subquery(Long.class);
                Root<MemberMembership> mm = activeMembershipSubquery.from(MemberMembership.class);
                activeMembershipSubquery.select(cb.literal(1L))
                        .where(
                                cb.equal(mm.get("member"), root),
                                cb.isTrue(mm.get("active")),
                                cb.greaterThanOrEqualTo(mm.get("endDate"), effectiveToday)
                        );

                Predicate noActiveTrialDate = cb.or(
                        cb.isNull(root.get("trialEndDate")),
                        cb.lessThan(root.get("trialEndDate"), effectiveToday)
                );
                predicates.add(cb.and(noActiveTrialDate, cb.not(cb.exists(activeMembershipSubquery))));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Join<Member, TenantUser> userJoin = root.join("user", JoinType.INNER);
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(userJoin.get("firstName"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(userJoin.get("lastName"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(userJoin.get("email"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(userJoin.get("phoneNumber"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("gymId"), "")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
