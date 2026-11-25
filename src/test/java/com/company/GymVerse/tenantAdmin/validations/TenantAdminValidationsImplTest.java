package com.company.GymVerse.tenantAdmin.validations;

import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.exceptions.common.ConflictException;
import com.company.GymVerse.models.tenant.enums.DiscountType;
import com.company.GymVerse.models.tenant.enums.PlanTheme;
import com.company.GymVerse.repositories.tenant.MembershipPlanRepository;
import com.company.GymVerse.repositories.tenant.PlanTierRepository;
import com.company.GymVerse.tenantAdmin.request.plans.CreatePlanRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantAdminValidationsImplTest {

    private Set<String> existingPlanNames;
    private Set<String> existingTierNames;
    private TenantAdminValidationsImpl validations;
    private final Set<String> allowedFields = Set.of("id", "name", "basePrice");

    @BeforeEach
    void setUp() {
        existingPlanNames = new HashSet<>();
        existingTierNames = new HashSet<>(Set.of("ELITE", "LEGENDARY"));

        MembershipPlanRepository planRepo = (MembershipPlanRepository) Proxy.newProxyInstance(
                MembershipPlanRepository.class.getClassLoader(),
                new Class<?>[]{MembershipPlanRepository.class},
                (proxy, method, args) -> "existsByNameIgnoreCase".equals(method.getName())
                        && existingPlanNames.contains(((String) args[0]).toLowerCase())
        );
        PlanTierRepository tierRepo = (PlanTierRepository) Proxy.newProxyInstance(
                PlanTierRepository.class.getClassLoader(),
                new Class<?>[]{PlanTierRepository.class},
                (proxy, method, args) -> "existsByNameIgnoreCase".equals(method.getName())
                        && existingTierNames.contains(((String) args[0]).toUpperCase())
        );

        validations = new TenantAdminValidationsImpl(planRepo, tierRepo);
    }

    @Test
    @DisplayName("Builds default Pageable when optional pagination fields are null")
    void validateAndBuildPageable_Defaults() {
        Pageable pageable = validations.validateAndBuildPageable(null, null, null, null, "id", allowedFields);
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("id")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("Rejects invalid page, size, sort field, and sort direction")
    void validateAndBuildPageable_InvalidInputs() {
        assertThatThrownBy(() -> validations.validateAndBuildPageable(-1, 10, "id", "asc", "id", allowedFields))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> validations.validateAndBuildPageable(0, 101, "id", "asc", "id", allowedFields))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> validations.validateAndBuildPageable(0, 10, "secretField", "asc", "id", allowedFields))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> validations.validateAndBuildPageable(0, 10, "id", "sideways", "id", allowedFields))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Validates custom tier theme requirement and rejects default themes on custom tiers")
    void validateCreatePlan_TierThemeGuards() {
        CreatePlanRequest missingCustomTheme = CreatePlanRequest.builder()
                .name("Titan Annual")
                .tier("TITAN")
                .facilities(List.of("Weightlifting"))
                .basePrice(new BigDecimal("199.00"))
                .durationDays(365)
                .build();
        assertThatThrownBy(() -> validations.validateCreatePlan(missingCustomTheme))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("customTheme is required");

        CreatePlanRequest reservedTheme = CreatePlanRequest.builder()
                .name("Titan Annual")
                .tier("TITAN")
                .customTheme(PlanTheme.GOLDEN)
                .facilities(List.of("Weightlifting"))
                .basePrice(new BigDecimal("199.00"))
                .durationDays(365)
                .build();
        assertThatThrownBy(() -> validations.validateCreatePlan(reservedTheme))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("reserved for default tiers");
    }

    @Test
    @DisplayName("Rejects duplicate plan names and out-of-bounds discounts")
    void validateCreatePlan_DuplicateAndDiscountGuards() {
        existingPlanNames.add("elite monthly");
        CreatePlanRequest duplicate = CreatePlanRequest.builder()
                .name("Elite Monthly")
                .tier("ELITE")
                .facilities(List.of("Cardio"))
                .basePrice(new BigDecimal("50.00"))
                .durationDays(30)
                .build();
        assertThatThrownBy(() -> validations.validateCreatePlan(duplicate))
                .isInstanceOf(ConflictException.class);

        CreatePlanRequest badDiscount = CreatePlanRequest.builder()
                .name("Fresh Plan")
                .tier("ELITE")
                .facilities(List.of("Cardio"))
                .basePrice(new BigDecimal("50.00"))
                .discountType(DiscountType.FLAT)
                .discountValue(new BigDecimal("60.00"))
                .durationDays(30)
                .build();
        assertThatThrownBy(() -> validations.validateCreatePlan(badDiscount))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Flat discount cannot exceed basePrice");
    }
}
