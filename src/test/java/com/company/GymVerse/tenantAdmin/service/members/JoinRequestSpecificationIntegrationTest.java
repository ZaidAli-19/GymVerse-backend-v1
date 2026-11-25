package com.company.GymVerse.tenantAdmin.service.members;

import com.company.GymVerse.models.central.enums.TenantRole;
import com.company.GymVerse.models.tenant.JoinRequest;
import com.company.GymVerse.models.tenant.TenantUser;
import com.company.GymVerse.models.tenant.enums.JoinRequestStatus;
import com.company.GymVerse.multitanancy.TenantContext;
import com.company.GymVerse.multitanancy.TenantDataSourceCache;
import com.company.GymVerse.multitanancy.TenantDatabaseService;
import com.company.GymVerse.multitanancy.TenantFlywayService;
import com.company.GymVerse.multitanancy.TenantRoutingBridge;
import com.company.GymVerse.repositories.tenant.JoinRequestRepository;
import com.company.GymVerse.repositories.tenant.TenantUserRepository;
import com.company.GymVerse.tenantAdmin.request.TenantAdminRequest;
import com.company.GymVerse.tenantAdmin.request.enums.TenantAdminRequestType;
import com.company.GymVerse.tenantAdmin.request.members.FetchJoinRequestsRequest;
import com.company.GymVerse.tenantAdmin.request.members.RejectJoinRequest;
import com.company.GymVerse.tenantAdmin.response.TenantAdminResponse;
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

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Story 5.3: JoinRequest JPA Specification & Reject MySQL Integration Test")
class JoinRequestSpecificationIntegrationTest {

    private static final String TENANT_DB = "gym_db_jr_test_001";

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
    private JoinRequestRepository joinRequestRepository;

    @Autowired
    private FetchJoinRequestsService fetchJoinRequestsService;

    @Autowired
    private RejectJoinRequestService rejectJoinRequestService;

    @BeforeEach
    void setUp() {
        tenantDataSourceCache.evict(TENANT_DB);
        tenantDatabaseService.dropDatabase(TENANT_DB);
        tenantDatabaseService.createDatabase(TENANT_DB);
        tenantFlywayService.migrate(TENANT_DB);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("9001", null, List.of(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        tenantDataSourceCache.evict(TENANT_DB);
        tenantDatabaseService.dropDatabase(TENANT_DB);
    }

    @Test
    @DisplayName("Filters join requests via JoinRequestSpecification and rejects pending request in MySQL")
    void fetchAndRejectJoinRequestsAgainstMysql() {
        routingBridge.runInTenant(TENANT_DB, () -> {
            tenantUserRepository.saveAndFlush(TenantUser.builder()
                    .centralUserId(9001L)
                    .firstName("Admin")
                    .lastName("One")
                    .email("admin@gym.com")
                    .role(TenantRole.TENANT_ADMIN)
                    .active(true)
                    .build());

            JoinRequest pending = joinRequestRepository.saveAndFlush(JoinRequest.builder()
                    .centralUserId(5001L)
                    .email("sara@gym.com")
                    .firstName("Sara")
                    .lastName("Connor")
                    .phoneNumber("555-7777")
                    .status(JoinRequestStatus.PENDING)
                    .requestNotes("Interested in evening strength training")
                    .createdAt(Instant.now())
                    .build());

            joinRequestRepository.saveAndFlush(JoinRequest.builder()
                    .centralUserId(5002L)
                    .email("john@gym.com")
                    .firstName("John")
                    .lastName("Doe")
                    .status(JoinRequestStatus.APPROVED)
                    .createdAt(Instant.now().minusSeconds(60))
                    .build());

            TenantAdminResponse filtered = fetchJoinRequestsService.execute(TenantAdminRequest.builder()
                    .tenantAdminRequestType(TenantAdminRequestType.FETCH_JOIN_REQUESTS)
                    .fetchJoinRequestsRequest(FetchJoinRequestsRequest.builder()
                            .status("PENDING")
                            .search("connor")
                            .build())
                    .build());
            assertThat(filtered.getJoinRequestsPageResponse().totalElements()).isEqualTo(1L);
            assertThat(filtered.getJoinRequestsPageResponse().content().get(0).getEmail()).isEqualTo("sara@gym.com");

            TenantAdminResponse rejected = rejectJoinRequestService.execute(TenantAdminRequest.builder()
                    .tenantAdminRequestType(TenantAdminRequestType.REJECT_JOIN_REQUEST)
                    .rejectJoinRequest(RejectJoinRequest.builder()
                            .joinRequestId(pending.getId())
                            .adminNotes("Gym at capacity for evening slot")
                            .build())
                    .build());
            assertThat(rejected.getJoinRequestItemResponse().getStatus()).isEqualTo(JoinRequestStatus.REJECTED);
        });
    }
}
