package com.company.GymVerse.tenantAdmin.service.trainers;

import com.company.GymVerse.attendance.service.AttendanceQueryPort;
import com.company.GymVerse.attendance.service.AttendanceQueryStubImpl;
import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.models.central.enums.TenantRole;
import com.company.GymVerse.models.tenant.TenantUser;
import com.company.GymVerse.models.tenant.Trainer;
import com.company.GymVerse.multitanancy.TenantContext;
import com.company.GymVerse.repositories.tenant.MemberRepository;
import com.company.GymVerse.repositories.tenant.MembershipPlanRepository;
import com.company.GymVerse.repositories.tenant.PlanTierRepository;
import com.company.GymVerse.repositories.tenant.TrainerRepository;
import com.company.GymVerse.tenantAdmin.mapper.TenantAdminMapperImpl;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.trainers.FetchTrainersRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.trainers.TrainerItemResponse;
import com.company.GymVerse.tenantAdmin.response.trainers.TrainersSummaryResponse;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Story 5.5: Trainers & Staff Summary & Address Book Unit Tests")
class TrainersServiceTest {

    private Map<Long, Trainer> trainersById;
    private Map<Long, Long> assignedMemberCountsByUserId;

    private FetchTrainersSummaryService fetchTrainersSummaryService;
    private FetchTrainersService fetchTrainersService;

    @BeforeEach
    void setUp() {
        trainersById = new LinkedHashMap<>();
        assignedMemberCountsByUserId = new LinkedHashMap<>();

        LocalDate today = LocalDate.now();
        Trainer activeTrainer = Trainer.builder()
                .id(1L)
                .user(TenantUser.builder()
                        .id(201L).centralUserId(3001L).firstName("Marcus").lastName("Vance")
                        .email("marcus@gym.com").phoneNumber("555-8001").role(TenantRole.TRAINER).active(true).build())
                .specialization("Strength & Conditioning")
                .experienceYears(8)
                .bio("Certified CSCS powerlifting coach")
                .joinedDate(today.minusMonths(6))
                .active(true)
                .build();
        trainersById.put(1L, activeTrainer);
        assignedMemberCountsByUserId.put(201L, 12L);

        Trainer inactiveTrainer = Trainer.builder()
                .id(2L)
                .user(TenantUser.builder()
                        .id(202L).centralUserId(3002L).firstName("Elena").lastName("Rostova")
                        .email("elena@gym.com").phoneNumber("555-8002").role(TenantRole.TRAINER).active(false).build())
                .specialization("HIIT & Mobility")
                .experienceYears(5)
                .bio("Mobility specialist on sabbatical")
                .joinedDate(today.minusMonths(12))
                .active(false)
                .build();
        trainersById.put(2L, inactiveTrainer);
        assignedMemberCountsByUserId.put(202L, 0L);

        TrainerRepository trainerRepo = (TrainerRepository) Proxy.newProxyInstance(
                TrainerRepository.class.getClassLoader(),
                new Class<?>[]{TrainerRepository.class},
                (p, m, args) -> switch (m.getName()) {
                    case "count" -> (long) trainersById.size();
                    case "countByActiveTrue" -> trainersById.values().stream().filter(Trainer::isActive).count();
                    case "findAll" -> {
                        if (args != null && args.length == 2 && args[0] instanceof Specification<?> && args[1] instanceof Pageable pageable) {
                            List<Trainer> all = new ArrayList<>(trainersById.values());
                            yield new PageImpl<>(all, pageable, all.size());
                        }
                        yield new ArrayList<>(trainersById.values());
                    }
                    default -> throw new UnsupportedOperationException(m.getName());
                }
        );

        MemberRepository memberRepo = (MemberRepository) Proxy.newProxyInstance(
                MemberRepository.class.getClassLoader(),
                new Class<?>[]{MemberRepository.class},
                (p, m, args) -> switch (m.getName()) {
                    case "countAssignedMembersByTrainerUserIds" -> {
                        @SuppressWarnings("unchecked")
                        Collection<Long> ids = (Collection<Long>) args[0];
                        List<Object[]> rows = new ArrayList<>();
                        for (Long userId : ids) {
                            Long count = assignedMemberCountsByUserId.get(userId);
                            if (count != null && count > 0) {
                                rows.add(new Object[]{userId, count});
                            }
                        }
                        yield rows;
                    }
                    default -> throw new UnsupportedOperationException(m.getName());
                }
        );

        MembershipPlanRepository planRepo = (MembershipPlanRepository) Proxy.newProxyInstance(
                MembershipPlanRepository.class.getClassLoader(),
                new Class<?>[]{MembershipPlanRepository.class},
                (p, m, args) -> false
        );
        PlanTierRepository tierRepo = (PlanTierRepository) Proxy.newProxyInstance(
                PlanTierRepository.class.getClassLoader(),
                new Class<?>[]{PlanTierRepository.class},
                (p, m, args) -> false
        );

        AttendanceQueryPort attendanceQueryPort = new AttendanceQueryStubImpl();
        TenantAdminValidationsImpl validations = new TenantAdminValidationsImpl(planRepo, tierRepo);
        TenantAdminMapperImpl mapper = new TenantAdminMapperImpl();

        fetchTrainersSummaryService = new FetchTrainersSummaryService(trainerRepo, attendanceQueryPort, validations, mapper);
        fetchTrainersService = new FetchTrainersService(trainerRepo, memberRepo, validations, mapper);

        TenantContext.setCurrentTenant("gym_db_test");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("9001", null, List.of(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN")))
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("FETCH_TRAINERS_SUMMARY returns total/active trainer counts and stubs live/attendance metrics via AttendanceQueryPort")
    void fetchTrainersSummary_Success() {
        TenantAdminResponse response = fetchTrainersSummaryService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_TRAINERS_SUMMARY)
                .build());

        TrainersSummaryResponse summary = response.getTrainersSummaryResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(summary.getTotalTrainersCount()).isEqualTo(2L);
        assertThat(summary.getActiveTrainersCount()).isEqualTo(1L);
        assertThat(summary.getActiveLiveCount()).isZero();
        assertThat(summary.getAvgAttendanceRate()).isZero();
    }

    @Test
    @DisplayName("FETCH_TRAINERS maps trainer profiles, assigned member counts, pagination, and rejects invalid sortBy")
    void fetchTrainers_MappingPaginationAndSortGuards() {
        TenantAdminResponse res = fetchTrainersService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_TRAINERS)
                .fetchTrainersRequest(FetchTrainersRequest.builder()
                        .active(true)
                        .search("strength")
                        .page(0)
                        .size(10)
                        .sortBy("user.firstName")
                        .sortDirection("asc")
                        .build())
                .build());

        assertThat(res.getTrainersPageResponse().totalElements()).isEqualTo(2L);
        List<TrainerItemResponse> content = res.getTrainersPageResponse().content();
        TrainerItemResponse first = content.get(0);
        assertThat(first.getTrainerId()).isEqualTo(1L);
        assertThat(first.getUserId()).isEqualTo(201L);
        assertThat(first.getFirstName()).isEqualTo("Marcus");
        assertThat(first.getLastName()).isEqualTo("Vance");
        assertThat(first.getSpecialization()).isEqualTo("Strength & Conditioning");
        assertThat(first.getExperienceYears()).isEqualTo(8);
        assertThat(first.getAssignedMembersCount()).isEqualTo(12L);
        assertThat(first.isActive()).isTrue();

        assertThat(content.get(1).getAssignedMembersCount()).isZero();

        assertThatThrownBy(() -> fetchTrainersService.execute(TenantAdminRequest.builder()
                .tenantAdminRequestType(TenantAdminRequestType.FETCH_TRAINERS)
                .fetchTrainersRequest(FetchTrainersRequest.builder().sortBy("invalidField").build())
                .build()))
                .isInstanceOf(BadRequestException.class);
    }
}
