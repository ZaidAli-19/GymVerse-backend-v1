package com.company.GymVerse.platformAdmin;

import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.enums.GymStatus;
import com.company.GymVerse.multitanancy.TenantContext;
import com.company.GymVerse.platformAdmin.factory.PlatformAdminFactory;
import com.company.GymVerse.platformAdmin.factory.PlatformAdminFactoryOperation;
import com.company.GymVerse.platformAdmin.request.GymTenantFilterRequest;
import com.company.GymVerse.platformAdmin.request.PlatformAdminRequest;
import com.company.GymVerse.platformAdmin.response.PlatformAdminResponse;
import com.company.GymVerse.platformAdmin.util.PlatformAdminRequestType;
import com.company.GymVerse.repositories.central.GymTenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("Story PA-6: Platform Registered Gym Tenants Dynamic Directory Retrieval Integration Tests")
class GetGymTenantsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GymTenantRepository gymTenantRepository;

    @Autowired
    private PlatformAdminFactory platformAdminFactory;

    private static final String TENANT_PREFIX = "PA6_TEST_";
    private static final String CODE_PREFIX = "PA6_";

    private GymTenant tenantA;
    private GymTenant tenantB;
    private GymTenant tenantC;
    private GymTenant tenantD;

    @BeforeEach
    void setUp() {
        cleanTestData();

        Instant baseTime = Instant.now().minusSeconds(1000);

        // Tenant A: Austin, ACTIVE, Iron Fitness
        tenantA = gymTenantRepository.save(GymTenant.builder()
                .gymName(TENANT_PREFIX + "Iron Fitness")
                .contactEmail("admin@ironfitness.com")
                .phoneNumber("+1-512-555-0199")
                .address("100 Muscle Way")
                .city("Austin")
                .logoUrl("https://assets.ironfitness.com/logo.png")
                .gymCode(CODE_PREFIX + "IRONFIT")
                .dbName("gym_db_pa6_1")
                .status(GymStatus.ACTIVE)
                .registeredAt(baseTime.plusSeconds(100))
                .build());

        // Tenant B: Dallas, ACTIVE, Metro Health
        tenantB = gymTenantRepository.save(GymTenant.builder()
                .gymName(TENANT_PREFIX + "Metro Health")
                .contactEmail("info@metrohealth.com")
                .phoneNumber("+1-214-555-0188")
                .address("200 Metro Blvd")
                .city("Dallas")
                .logoUrl("https://assets.metrohealth.com/logo.png")
                .gymCode(CODE_PREFIX + "METRO")
                .dbName("gym_db_pa6_2")
                .status(GymStatus.ACTIVE)
                .registeredAt(baseTime.plusSeconds(200))
                .build());

        // Tenant C: Austin, SUSPENDED, Austin Fit
        tenantC = gymTenantRepository.save(GymTenant.builder()
                .gymName(TENANT_PREFIX + "Austin Fit")
                .contactEmail("austin@fit.com")
                .phoneNumber("+1-512-555-0177")
                .address("300 Austin Lane")
                .city("Austin")
                .logoUrl("https://assets.austinfit.com/logo.png")
                .gymCode(CODE_PREFIX + "AUSFIT")
                .dbName("gym_db_pa6_3")
                .status(GymStatus.SUSPENDED)
                .registeredAt(baseTime.plusSeconds(300))
                .suspendedAt(Instant.now())
                .suspensionReason("Payment default")
                .build());

        // Tenant D: Houston, INACTIVE, Houston Power
        tenantD = gymTenantRepository.save(GymTenant.builder()
                .gymName(TENANT_PREFIX + "Houston Power")
                .contactEmail("houston@power.com")
                .phoneNumber("+1-713-555-0166")
                .address("400 Power Rd")
                .city("Houston")
                .logoUrl(null)
                .gymCode(CODE_PREFIX + "HOUSTON")
                .dbName("gym_db_pa6_4")
                .status(GymStatus.INACTIVE)
                .registeredAt(baseTime.plusSeconds(400))
                .build());
    }

    @AfterEach
    void tearDown() {
        cleanTestData();
    }

    private void cleanTestData() {
        List<GymTenant> testTenants = gymTenantRepository.findAll().stream()
                .filter(t -> (t.getGymName() != null && t.getGymName().startsWith(TENANT_PREFIX))
                        || (t.getGymCode() != null && t.getGymCode().startsWith(CODE_PREFIX)))
                .toList();
        gymTenantRepository.deleteAll(testTenants);
    }

    @Test
    @DisplayName("AC-PA6.1: Dynamic filtering by status and city")
    void testDynamicFilterByStatusAndCity() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_GYM_TENANTS)
                .gymName(TENANT_PREFIX)
                .gymStatus(GymStatus.ACTIVE)
                .city("austin")
                .page(0)
                .size(10)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage", is(0)))
                .andExpect(jsonPath("$.totalRecords", is(1)))
                .andExpect(jsonPath("$.tenants", hasSize(1)))
                .andExpect(jsonPath("$.tenants[0].gymName", is(TENANT_PREFIX + "Iron Fitness")))
                .andExpect(jsonPath("$.tenants[0].city", is("Austin")))
                .andExpect(jsonPath("$.tenants[0].status", is("ACTIVE")))
                .andExpect(jsonPath("$.tenants[0].gymCode", is(CODE_PREFIX + "IRONFIT")));
    }

    @Test
    @DisplayName("AC-PA6.2: Search by gymCode prefix and partial gymName")
    void testSearchByGymCodePrefixAndPartialName() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_GYM_TENANTS)
                .gymCode(CODE_PREFIX.toLowerCase() + "iron")
                .gymName("fit")
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenants", hasSize(1)))
                .andExpect(jsonPath("$.tenants[0].gymName", is(TENANT_PREFIX + "Iron Fitness")))
                .andExpect(jsonPath("$.tenants[0].gymCode", is(CODE_PREFIX + "IRONFIT")));
    }

    @Test
    @DisplayName("AC-PA6.3: Whitelist-guarded sorting by registeredAt descending")
    void testWhitelistGuardedSortingRegisteredAtDesc() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_GYM_TENANTS)
                .gymName(TENANT_PREFIX)
                .sortBy("registeredAt")
                .sortDirection("desc")
                .page(0)
                .size(10)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenants", hasSize(4)))
                .andExpect(jsonPath("$.tenants[0].gymName", is(TENANT_PREFIX + "Houston Power")))
                .andExpect(jsonPath("$.tenants[1].gymName", is(TENANT_PREFIX + "Austin Fit")))
                .andExpect(jsonPath("$.tenants[2].gymName", is(TENANT_PREFIX + "Metro Health")))
                .andExpect(jsonPath("$.tenants[3].gymName", is(TENANT_PREFIX + "Iron Fitness")));
    }

    @Test
    @DisplayName("AC-PA6.3: Whitelist-guarded sorting by gymName ascending")
    void testWhitelistGuardedSortingGymNameAsc() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_GYM_TENANTS)
                .gymName(TENANT_PREFIX)
                .sortBy("gymName")
                .sortDirection("asc")
                .page(0)
                .size(10)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenants", hasSize(4)))
                .andExpect(jsonPath("$.tenants[0].gymName", is(TENANT_PREFIX + "Austin Fit")))
                .andExpect(jsonPath("$.tenants[1].gymName", is(TENANT_PREFIX + "Houston Power")))
                .andExpect(jsonPath("$.tenants[2].gymName", is(TENANT_PREFIX + "Iron Fitness")))
                .andExpect(jsonPath("$.tenants[3].gymName", is(TENANT_PREFIX + "Metro Health")));
    }

    @Test
    @DisplayName("AC-PA6.3: Invalid sort property rejected with HTTP 400")
    void testInvalidSortFieldRejected() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_GYM_TENANTS)
                .sortBy("contactEmail")
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid sort field: 'contactEmail'")))
                .andExpect(jsonPath("$.message", containsString("[id, gymName, gymCode, city, status, registeredAt]")));
    }

    @Test
    @DisplayName("AC-PA6.3: Invalid sort direction rejected with HTTP 400")
    void testInvalidSortDirectionRejected() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_GYM_TENANTS)
                .sortDirection("diagonal")
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid sort direction: 'diagonal'")));
    }

    @Test
    @DisplayName("AC-PA6.4: Validated pagination boundaries")
    void testPaginationBoundaries() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_GYM_TENANTS)
                .gymName(TENANT_PREFIX)
                .sortBy("gymName")
                .sortDirection("asc")
                .page(0)
                .size(2)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage", is(0)))
                .andExpect(jsonPath("$.pageSize", is(2)))
                .andExpect(jsonPath("$.totalRecords", is(4)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.last", is(false)))
                .andExpect(jsonPath("$.tenants", hasSize(2)))
                .andExpect(jsonPath("$.pagedTenants.content", hasSize(2)))
                .andExpect(jsonPath("$.pagedTenants.totalElements", is(4)))
                .andExpect(jsonPath("$.pagedTenants.totalPages", is(2)))
                .andExpect(jsonPath("$.pagedTenants.last", is(false)));

        // Second page
        PlatformAdminRequest page2 = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_GYM_TENANTS)
                .gymName(TENANT_PREFIX)
                .sortBy("gymName")
                .sortDirection("asc")
                .page(1)
                .size(2)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(page2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage", is(1)))
                .andExpect(jsonPath("$.last", is(true)))
                .andExpect(jsonPath("$.tenants", hasSize(2)));
    }

    @Test
    @DisplayName("AC-PA6.4: Invalid pagination values rejected with HTTP 400")
    void testInvalidPaginationRejected() throws Exception {
        // Negative page index
        PlatformAdminRequest negPage = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_GYM_TENANTS)
                .page(-1)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negPage)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Page index must not be negative")));

        // Zero page size
        PlatformAdminRequest zeroSize = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_GYM_TENANTS)
                .size(0)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroSize)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Page size must be between 1 and 100")));

        // Size > 100
        PlatformAdminRequest excessSize = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_GYM_TENANTS)
                .size(101)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessSize)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Page size must be between 1 and 100")));
    }

    @Test
    @DisplayName("AC-PA6.5: Zero Tenant Database Routing Invariant")
    void testZeroTenantRoutingInvariant() throws Exception {
        // TenantContext must remain null before and after query execution
        assertThat(TenantContext.getCurrentTenant()).isNull();

        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_GYM_TENANTS)
                .gymName(TENANT_PREFIX)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    @DisplayName("AC-PA6.6: Empty results structure when no tenants match filters")
    void testEmptyResultsStructure() throws Exception {
        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_GYM_TENANTS)
                .gymName("NON_EXISTENT_GYM_NAME_XYZ")
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenants", is(empty())))
                .andExpect(jsonPath("$.totalRecords", is(0)))
                .andExpect(jsonPath("$.totalPages", is(0)))
                .andExpect(jsonPath("$.last", is(true)));
    }

    @Test
    @DisplayName("Direct Factory Resolution: PlatformAdminFactory resolves GetGymTenantsService")
    void testDirectFactoryResolution() {
        PlatformAdminFactoryOperation operation = platformAdminFactory.getOperation(PlatformAdminRequestType.FETCH_ALL_GYM_TENANTS);
        assertThat(operation).isNotNull();
        assertThat(operation.getRequestType()).isEqualTo(PlatformAdminRequestType.FETCH_ALL_GYM_TENANTS);

        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_GYM_TENANTS)
                .gymName(TENANT_PREFIX)
                .page(0)
                .size(5)
                .build();

        PlatformAdminResponse response = operation.execute(request);
        assertThat(response).isNotNull();
        assertThat(response.getTenants()).hasSize(4);
        assertThat(response.getTotalRecords()).isEqualTo(4L);
    }

    @Test
    @DisplayName("Nested tenantFilter object support: PlatformAdminRequest with tenantFilter")
    void testNestedTenantFilterSupport() throws Exception {
        GymTenantFilterRequest filter = GymTenantFilterRequest.builder()
                .gymName(TENANT_PREFIX + "Houston")
                .status(GymStatus.INACTIVE)
                .city("Houston")
                .gymCode(CODE_PREFIX + "HOU")
                .page(0)
                .size(5)
                .sortBy("registeredAt")
                .sortDirection("desc")
                .build();

        PlatformAdminRequest request = PlatformAdminRequest.builder()
                .platformAdminRequestType(PlatformAdminRequestType.FETCH_ALL_GYM_TENANTS)
                .tenantFilter(filter)
                .build();

        mockMvc.perform(post("/api/platform-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenants", hasSize(1)))
                .andExpect(jsonPath("$.tenants[0].gymName", is(TENANT_PREFIX + "Houston Power")))
                .andExpect(jsonPath("$.tenants[0].status", is("INACTIVE")));
    }
}
