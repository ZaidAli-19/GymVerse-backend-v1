package com.company.GymVerse.tenantAdmin.service.plans;

import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.models.tenant.GymFacility;
import com.company.GymVerse.models.tenant.MembershipPlan;
import com.company.GymVerse.models.tenant.PlanTier;
import com.company.GymVerse.models.tenant.enums.DiscountType;
import com.company.GymVerse.models.tenant.enums.PlanTheme;
import com.company.GymVerse.multitanancy.TenantContext;
import com.company.GymVerse.repositories.tenant.GymFacilityRepository;
import com.company.GymVerse.repositories.tenant.MembershipPlanRepository;
import com.company.GymVerse.repositories.tenant.PlanTierRepository;
import com.company.GymVerse.tenantAdmin.helper.PlanHelper;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapperImpl;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.plans.CreatePlanRequest;
import com.company.GymVerse.tenantAdmin.request.plans.FetchPlansRequest;
import com.company.GymVerse.tenantAdmin.request.plans.UpdatePlanAvailabilityRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.plans.PlanFilterOptionsResponse;
import com.company.GymVerse.tenantAdmin.response.plans.PlanItemResponse;
import com.company.GymVerse.tenantAdmin.response.plans.PlansSummaryResponse;
import com.company.GymVerse.tenantAdmin.validations.TenantAdminValidationsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Story 5.2: Plans Management Suite Unit Tests")
class PlansServiceTest {

    private Map<Long, PlanTier> tiersById;
    private Map<Long, GymFacility> facilitiesById;
    private Map<Long, MembershipPlan> plansById;
    private AtomicLong tierSeq;
    private AtomicLong facilitySeq;
    private AtomicLong planSeq;

    private FetchPlansSummaryService fetchPlansSummaryService;
    private FetchPlanFilterOptionsService fetchPlanFilterOptionsService;
    private FetchPlansService fetchPlansService;
    private CreatePlanService createPlanService;
    private UpdatePlanAvailabilityService updatePlanAvailabilityService;

    @BeforeEach
    void setUp() {
        tiersById = new LinkedHashMap<>();
        facilitiesById = new LinkedHashMap<>();
        plansById = new LinkedHashMap<>();
        tierSeq = new AtomicLong(10);
        facilitySeq = new AtomicLong(1);
        planSeq = new AtomicLong(1);

        tiersById.put(1L, PlanTier.builder().id(1L).name("ELITE").themeKey(PlanTheme.GOLDEN).defaultTier(true).build());
        tiersById.put(2L, PlanTier.builder().id(2L).name("LEGENDARY").themeKey(PlanTheme.PURPLE).defaultTier(true).build());

        PlanTierRepository tierRepo = (PlanTierRepository) Proxy.newProxyInstance(
                PlanTierRepository.class.getClassLoader(),
                new Class<?>[]{PlanTierRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByNameIgnoreCase" -> tiersById.values().stream()
                            .filter(t -> t.getName().equalsIgnoreCase((String) args[0]))
                            .findFirst();
                    case "existsByNameIgnoreCase" -> tiersById.values().stream()
                            .anyMatch(t -> t.getName().equalsIgnoreCase((String) args[0]));
                    case "findAllByOrderByDefaultTierDescNameAsc" -> tiersById.values().stream()
                            .sorted(Comparator.comparing(PlanTier::isDefaultTier).reversed()
                                    .thenComparing(PlanTier::getName))
                            .toList();
                    case "save" -> {
                        PlanTier tier = (PlanTier) args[0];
                        if (tier.getId() == null) {
                            tier.setId(tierSeq.incrementAndGet());
                        }
                        tiersById.put(tier.getId(), tier);
                        yield tier;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );

        GymFacilityRepository facilityRepo = (GymFacilityRepository) Proxy.newProxyInstance(
                GymFacilityRepository.class.getClassLoader(),
                new Class<?>[]{GymFacilityRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByNameIgnoreCase" -> facilitiesById.values().stream()
                            .filter(f -> f.getName().equalsIgnoreCase((String) args[0]))
                            .findFirst();
                    case "findAllByOrderByNameAsc" -> facilitiesById.values().stream()
                            .sorted(Comparator.comparing(GymFacility::getName, String.CASE_INSENSITIVE_ORDER))
                            .toList();
                    case "save" -> {
                        GymFacility facility = (GymFacility) args[0];
                        if (facility.getId() == null) {
                            facility.setId(facilitySeq.incrementAndGet());
                        }
                        facilitiesById.put(facility.getId(), facility);
                        yield facility;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );

        MembershipPlanRepository planRepo = (MembershipPlanRepository) Proxy.newProxyInstance(
                MembershipPlanRepository.class.getClassLoader(),
                new Class<?>[]{MembershipPlanRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "existsByNameIgnoreCase" -> plansById.values().stream()
                            .anyMatch(p -> p.getName().equalsIgnoreCase((String) args[0]));
                    case "findAll" -> {
                        if (args != null && args.length == 2 && args[0] instanceof Specification<?> && args[1] instanceof Pageable pageable) {
                            List<MembershipPlan> all = new ArrayList<>(plansById.values());
                            yield new PageImpl<>(all, pageable, all.size());
                        }
                        yield new ArrayList<>(plansById.values());
                    }
                    case "findById" -> Optional.ofNullable(plansById.get((Long) args[0]));
                    case "save" -> {
                        MembershipPlan plan = (MembershipPlan) args[0];
                        if (plan.getId() == null) {
                            plan.setId(planSeq.incrementAndGet());
                        }
                        plansById.put(plan.getId(), plan);
                        yield plan;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );

        TenantAdminValidationsImpl validations = new TenantAdminValidationsImpl(planRepo, tierRepo);
        TenantAdminMapperImpl mapper = new TenantAdminMapperImpl();
        PlanHelper planHelper = new PlanHelper(tierRepo, facilityRepo);

        fetchPlansSummaryService = new FetchPlansSummaryService(tierRepo, planRepo, planHelper, validations, mapper);
        fetchPlanFilterOptionsService = new FetchPlanFilterOptionsService(tierRepo, facilityRepo, validations, mapper);
        fetchPlansService = new FetchPlansService(planRepo, validations, mapper);
        createPlanService = new CreatePlanService(planRepo, planHelper, validations, mapper);
        updatePlanAvailabilityService = new UpdatePlanAvailabilityService(planRepo, validations, mapper);

        TenantContext.setCurrentTenant("gym_db_test");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("2001", null, List.of(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN"))));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("CREATE_PLAN handles default ELITE tier, custom tier + custom theme, and auto-persists facilities")
    void createPlan_DefaultAndCustomTier() {
        TenantAdminRequest eliteReq = TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.CREATE_PLAN)
                .createPlanRequest(CreatePlanRequest.builder()
                        .name("Elite Monthly")
                        .description("Access to weights and cardio")
                        .tier("ELITE")
                        .facilities(List.of("Weightlifting", "Cardio", "cardio"))
                        .basePrice(new BigDecimal("100.00"))
                        .discountType(DiscountType.PERCENTAGE)
                        .discountValue(new BigDecimal("20"))
                        .durationDays(30)
                        .isTrial(false)
                        .build())
                .build();

        TenantAdminResponse eliteRes = createPlanService.execute(eliteReq);
        PlanItemResponse eliteItem = eliteRes.getPlanItemResponse();
        assertThat(eliteRes.getStatus()).isEqualTo(201);
        assertThat(eliteItem.getTierName()).isEqualTo("ELITE");
        assertThat(eliteItem.getThemeKey()).isEqualTo(PlanTheme.GOLDEN);
        assertThat(eliteItem.getFacilities()).containsExactly("Cardio", "Weightlifting");
        assertThat(eliteItem.getFinalPrice()).isEqualByComparingTo("80.00");
        assertThat(eliteItem.isHasOffer()).isTrue();

        TenantAdminRequest customReq = TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.CREATE_PLAN)
                .createPlanRequest(CreatePlanRequest.builder()
                        .name("Spartan Trial")
                        .tier("SPARTAN")
                        .customTheme(PlanTheme.EMERALD_GREEN)
                        .facilities(List.of("Cardio", "CrossFit"))
                        .basePrice(new BigDecimal("25.00"))
                        .durationDays(7)
                        .isTrial(true)
                        .build())
                .build();

        TenantAdminResponse customRes = createPlanService.execute(customReq);
        PlanItemResponse customItem = customRes.getPlanItemResponse();
        assertThat(customItem.getTierName()).isEqualTo("SPARTAN");
        assertThat(customItem.getThemeKey()).isEqualTo(PlanTheme.EMERALD_GREEN);
        assertThat(customItem.isTrial()).isTrue();
        assertThat(customItem.isHasOffer()).isFalse();
        assertThat(facilitiesById).hasSize(3); // Weightlifting, Cardio, CrossFit
    }

    @Test
    @DisplayName("FETCH_PLANS_SUMMARY and FETCH_PLAN_FILTER_OPTIONS return accurate counts, tiers, facilities, and 20 themes")
    void summaryAndFilterOptions() {
        createPlanService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.CREATE_PLAN)
                .createPlanRequest(CreatePlanRequest.builder()
                        .name("Legendary Pro")
                        .tier("LEGENDARY")
                        .facilities(List.of("Sauna", "Pool"))
                        .basePrice(new BigDecimal("150.00"))
                        .durationDays(30)
                        .build())
                .build());

        TenantAdminResponse summaryRes = fetchPlansSummaryService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_PLANS_SUMMARY)
                .build());
        PlansSummaryResponse summary = summaryRes.getPlansSummaryResponse();
        assertThat(summary.getTotalActivePlans()).isEqualTo(1L);
        assertThat(summary.getTierBreakdown())
                .anyMatch(t -> "LEGENDARY".equals(t.getTierName()) && t.getThemeKey() == PlanTheme.PURPLE && t.getActivePlanCount() == 1L);

        TenantAdminResponse optionsRes = fetchPlanFilterOptionsService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_PLAN_FILTER_OPTIONS)
                .build());
        PlanFilterOptionsResponse options = optionsRes.getPlanFilterOptionsResponse();
        assertThat(options.getTiers()).hasSize(2);
        assertThat(options.getFacilities()).containsExactly("Pool", "Sauna");
        assertThat(options.getCustomThemes()).hasSize(20);
    }

    @Test
    @DisplayName("UPDATE_PLAN_AVAILABILITY toggles plan active state and FETCH_PLANS returns paged envelope")
    void availabilityToggleAndFetchPlans() {
        TenantAdminResponse created = createPlanService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.CREATE_PLAN)
                .createPlanRequest(CreatePlanRequest.builder()
                        .name("Starter")
                        .tier("ELITE")
                        .facilities(List.of("Gym Floor"))
                        .basePrice(new BigDecimal("40.00"))
                        .durationDays(30)
                        .build())
                .build());
        Long planId = created.getPlanItemResponse().getId();

        TenantAdminResponse toggled = updatePlanAvailabilityService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.UPDATE_PLAN_AVAILABILITY)
                .updatePlanAvailabilityRequest(UpdatePlanAvailabilityRequest.builder()
                        .planId(planId)
                        .active(false)
                        .build())
                .build());
        assertThat(toggled.getPlanItemResponse().isActive()).isFalse();

        TenantAdminResponse fetched = fetchPlansService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_PLANS)
                .fetchPlansRequest(FetchPlansRequest.builder().page(0).size(10).sortBy("tier").sortDirection("asc").build())
                .build());
        assertThat(fetched.getPlansPageResponse().totalElements()).isEqualTo(1L);
        assertThat(fetched.getPlansPageResponse().content().get(0).isActive()).isFalse();

        assertThatThrownBy(() -> updatePlanAvailabilityService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.UPDATE_PLAN_AVAILABILITY)
                .updatePlanAvailabilityRequest(UpdatePlanAvailabilityRequest.builder().planId(999L).active(true).build())
                .build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
