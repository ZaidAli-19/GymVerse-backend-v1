package com.company.GymVerse.tenantAdmin.helper;

import com.company.GymVerse.models.tenant.GymFacility;
import com.company.GymVerse.models.tenant.MembershipPlan;
import com.company.GymVerse.models.tenant.PlanTier;
import com.company.GymVerse.models.tenant.enums.PlanTheme;
import com.company.GymVerse.repositories.tenant.GymFacilityRepository;
import com.company.GymVerse.repositories.tenant.PlanTierRepository;
import com.company.GymVerse.tenantAdmin.response.plans.TierCountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Helper component encapsulating plan tier resolution, facility persistence,
 * and O(N) tier summary aggregations so plan services remain concise orchestrators.
 */
@Component
@RequiredArgsConstructor
public class PlanHelper {

    private final PlanTierRepository planTierRepository;
    private final GymFacilityRepository gymFacilityRepository;

    /**
     * Resolves an existing tier by name or creates a new default/custom tier with its mapped theme.
     */
    public PlanTier resolveOrCreatePlanTier(final String tierName, final PlanTheme customTheme) {
        String normalizedName = tierName.trim().toUpperCase();
        Optional<PlanTier> existing = planTierRepository.findByNameIgnoreCase(normalizedName);
        if (existing.isPresent()) {
            return existing.get();
        }

        Optional<PlanTheme> defaultTheme = PlanTheme.resolveDefaultTierTheme(normalizedName);
        PlanTheme resolvedTheme = defaultTheme.orElse(customTheme);

        PlanTier newTier = PlanTier.builder()
                .name(normalizedName)
                .themeKey(resolvedTheme)
                .defaultTier(defaultTheme.isPresent())
                .build();

        return planTierRepository.save(newTier);
    }

    /**
     * Resolves existing facilities or persists newly introduced facility names cleanly.
     */
    public Set<GymFacility> resolveOrCreateFacilities(final List<String> facilityNames) {
        Set<GymFacility> resolved = new LinkedHashSet<>();
        Set<String> seenNormalized = new LinkedHashSet<>();

        for (String rawName : facilityNames) {
            if (rawName == null || rawName.trim().isBlank()) {
                continue;
            }
            String trimmed = rawName.trim();
            String key = trimmed.toLowerCase();
            if (!seenNormalized.add(key)) {
                continue;
            }

            GymFacility facility = gymFacilityRepository.findByNameIgnoreCase(trimmed)
                    .orElseGet(() -> gymFacilityRepository.save(GymFacility.builder().name(trimmed).build()));
            resolved.add(facility);
        }
        return resolved;
    }

    /**
     * Computes total and active plan counts per tier in a single pass.
     */
    public List<TierCountResponse> buildTierCounts(final List<PlanTier> tiers, final List<MembershipPlan> plans) {
        Map<Long, long[]> countsByTierId = new HashMap<>();
        for (MembershipPlan plan : plans) {
            if (plan.getPlanTier() == null || plan.getPlanTier().getId() == null) {
                continue;
            }
            long[] counts = countsByTierId.computeIfAbsent(plan.getPlanTier().getId(), id -> new long[2]);
            counts[0]++;
            if (plan.isActive()) {
                counts[1]++;
            }
        }

        List<TierCountResponse> result = new ArrayList<>(tiers.size());
        for (PlanTier tier : tiers) {
            long[] counts = countsByTierId.getOrDefault(tier.getId(), new long[]{0L, 0L});
            result.add(TierCountResponse.builder()
                    .tierName(tier.getName())
                    .themeKey(tier.getThemeKey())
                    .totalPlanCount(counts[0])
                    .activePlanCount(counts[1])
                    .build());
        }
        return result;
    }
}
