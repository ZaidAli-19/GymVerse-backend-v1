package com.company.GymVerse.tenantAdmin.service.trainers;

import com.company.GymVerse.models.central.enums.TenantRole;
import com.company.GymVerse.models.tenant.Member;
import com.company.GymVerse.models.tenant.TenantUser;
import com.company.GymVerse.models.tenant.Trainer;
import com.company.GymVerse.models.tenant.enums.MemberType;
import com.company.GymVerse.multitanancy.TenantContext;
import com.company.GymVerse.multitanancy.TenantDataSourceCache;
import com.company.GymVerse.multitanancy.TenantDatabaseService;
import com.company.GymVerse.multitanancy.TenantFlywayService;
import com.company.GymVerse.multitanancy.TenantRoutingBridge;
import com.company.GymVerse.repositories.tenant.MemberRepository;
import com.company.GymVerse.repositories.tenant.TenantUserRepository;
import com.company.GymVerse.repositories.tenant.TrainerRepository;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.trainers.FetchTrainersRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
import com.company.GymVerse.tenantAdmin.response.trainers.TrainersSummaryResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Story 5.5: Trainers JPA Specification & Assigned Member Count MySQL Integration Test")
class TrainersSpecificationIntegrationTest {

    private static final String TENANT_DB = "gym_db_trainers_test_001";

    @Autowired
    private TenantDatabaseService tenantDatabaseService;

    @Autowired
    private TenantFlywayService tenantFlywayService;

    @Autowired
    private TenantDataSourceCache tenantDataSourceCache;

    @Autowired
    private TenantRoutingBridge routingBridge;

    @Autowired
    private TenantUserRepository tenantUserRepository;

    @Autowired
    private TrainerRepository trainerRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FetchTrainersSummaryService fetchTrainersSummaryService;

    @Autowired
    private FetchTrainersService fetchTrainersService;

    @BeforeEach
    void setUp() {
        tenantDataSourceCache.evict(TENANT_DB);
        tenantDatabaseService.dropDatabase(TENANT_DB);
        tenantDatabaseService.createDatabase(TENANT_DB);
        tenantFlywayService.migrate(TENANT_DB);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("9001", null, List.of(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN")))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        tenantDataSourceCache.evict(TENANT_DB);
        tenantDatabaseService.dropDatabase(TENANT_DB);
    }

    @Test
    @DisplayName("Executes FETCH_TRAINERS_SUMMARY and FETCH_TRAINERS Specification filters/search/sorting against MySQL")
    void endToEndTrainersSummaryAndAddressBookAgainstMysql() {
        routingBridge.runInTenant(TENANT_DB, () -> {
            LocalDate today = LocalDate.now();

            TenantUser tUser1 = tenantUserRepository.saveAndFlush(TenantUser.builder()
                    .centralUserId(6001L).firstName("Marcus").lastName("Vance")
                    .email("marcus@gym.com").phoneNumber("555-8001").role(TenantRole.TRAINER).active(true).build());
            Trainer t1 = trainerRepository.saveAndFlush(Trainer.builder()
                    .user(tUser1).specialization("Strength & Conditioning").experienceYears(8)
                    .bio("Powerlifting coach").joinedDate(today.minusMonths(6)).active(true).build());

            TenantUser tUser2 = tenantUserRepository.saveAndFlush(TenantUser.builder()
                    .centralUserId(6002L).firstName("Elena").lastName("Rostova")
                    .email("elena@gym.com").phoneNumber("555-8002").role(TenantRole.TRAINER).active(true).build());
            trainerRepository.saveAndFlush(Trainer.builder()
                    .user(tUser2).specialization("HIIT & Mobility").experienceYears(5)
                    .bio("HIIT specialist").joinedDate(today.minusMonths(3)).active(true).build());

            TenantUser tUser3 = tenantUserRepository.saveAndFlush(TenantUser.builder()
                    .centralUserId(6003L).firstName("Viktor").lastName("Krum")
                    .email("viktor@gym.com").phoneNumber("555-8003").role(TenantRole.TRAINER).active(false).build());
            trainerRepository.saveAndFlush(Trainer.builder()
                    .user(tUser3).specialization("Boxing").experienceYears(10)
                    .bio("Former pro boxer").joinedDate(today.minusYears(1)).active(false).build());

            // Assign 2 members to Marcus
            TenantUser mUser1 = tenantUserRepository.saveAndFlush(TenantUser.builder()
                    .centralUserId(7001L).firstName("Alex").lastName("One")
                    .email("alex1@gym.com").role(TenantRole.MEMBER).active(true).build());
            memberRepository.saveAndFlush(Member.builder()
                    .user(mUser1).trainer(tUser1).joinDate(today).gymId("MEM-TR-001").type(MemberType.REGULAR).build());

            TenantUser mUser2 = tenantUserRepository.saveAndFlush(TenantUser.builder()
                    .centralUserId(7002L).firstName("Sam").lastName("Two")
                    .email("sam2@gym.com").role(TenantRole.MEMBER).active(true).build());
            memberRepository.saveAndFlush(Member.builder()
                    .user(mUser2).trainer(tUser1).joinDate(today).gymId("MEM-TR-002").type(MemberType.REGULAR).build());

            // 1. Summary check
            TenantAdminResponse summaryRes = fetchTrainersSummaryService.execute(TenantAdminRequest.builder()
                    .tenantAdminRequestType(TenantAdminRequestType.FETCH_TRAINERS_SUMMARY)
                    .build());
            TrainersSummaryResponse summary = summaryRes.getTrainersSummaryResponse();
            assertThat(summary.getTotalTrainersCount()).isEqualTo(3L);
            assertThat(summary.getActiveTrainersCount()).isEqualTo(2L);
            assertThat(summary.getActiveLiveCount()).isZero();
            assertThat(summary.getAvgAttendanceRate()).isZero();

            // 2. Active filter + sort by user.firstName
            TenantAdminResponse activeRes = fetchTrainersService.execute(TenantAdminRequest.builder()
                    .tenantAdminRequestType(TenantAdminRequestType.FETCH_TRAINERS)
                    .fetchTrainersRequest(FetchTrainersRequest.builder()
                            .active(true)
                            .sortBy("user.firstName")
                            .sortDirection("asc")
                            .build())
                    .build());
            assertThat(activeRes.getTrainersPageResponse().totalElements()).isEqualTo(2L);
            assertThat(activeRes.getTrainersPageResponse().content())
                    .extracting("firstName")
                    .containsExactly("Elena", "Marcus");
            assertThat(activeRes.getTrainersPageResponse().content().get(0).getAssignedMembersCount()).isZero();
            assertThat(activeRes.getTrainersPageResponse().content().get(1).getAssignedMembersCount()).isEqualTo(2L);
            assertThat(activeRes.getTrainersPageResponse().content().get(1).getTrainerId()).isEqualTo(t1.getId());

            // 3. Specialization filter + search
            TenantAdminResponse searchRes = fetchTrainersService.execute(TenantAdminRequest.builder()
                    .tenantAdminRequestType(TenantAdminRequestType.FETCH_TRAINERS)
                    .fetchTrainersRequest(FetchTrainersRequest.builder()
                            .specialization("boxing")
                            .search("krum")
                            .build())
                    .build());
            assertThat(searchRes.getTrainersPageResponse().totalElements()).isEqualTo(1L);
            assertThat(searchRes.getTrainersPageResponse().content().get(0).getEmail()).isEqualTo("viktor@gym.com");
        });
    }
}
