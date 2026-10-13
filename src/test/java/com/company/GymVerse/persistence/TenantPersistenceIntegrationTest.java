package com.company.GymVerse.persistence;

import com.company.GymVerse.models.central.enums.TenantRole;
import com.company.GymVerse.models.tenant.Attendance;
import com.company.GymVerse.models.tenant.CustomFriend;
import com.company.GymVerse.models.tenant.FriendRequest;
import com.company.GymVerse.models.tenant.GymConfig;
import com.company.GymVerse.models.tenant.GymInvitation;
import com.company.GymVerse.models.tenant.Member;
import com.company.GymVerse.models.tenant.MembershipPlan;
import com.company.GymVerse.models.tenant.TenantUser;
import com.company.GymVerse.models.tenant.enums.FriendRequestStatus;
import com.company.GymVerse.multitanancy.TenantContext;
import com.company.GymVerse.multitanancy.TenantDataSourceCache;
import com.company.GymVerse.multitanancy.TenantDatabaseService;
import com.company.GymVerse.multitanancy.TenantFlywayService;
import com.company.GymVerse.multitanancy.TenantRoutingBridge;
import com.company.GymVerse.repositories.tenant.AttendanceRepository;
import com.company.GymVerse.repositories.tenant.CustomFriendRepository;
import com.company.GymVerse.repositories.tenant.FriendRequestRepository;
import com.company.GymVerse.repositories.tenant.GymConfigRepository;
import com.company.GymVerse.repositories.tenant.GymInvitationRepository;
import com.company.GymVerse.repositories.tenant.MemberRepository;
import com.company.GymVerse.repositories.tenant.MembershipPlanRepository;
import com.company.GymVerse.repositories.tenant.TenantUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TenantPersistenceIntegrationTest {

    private static final String TENANT_DB = "gym_db_test_001";

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
    private GymConfigRepository gymConfigRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MembershipPlanRepository planRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private GymInvitationRepository invitationRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private CustomFriendRepository customFriendRepository;

    @BeforeEach
    void setUp() {
        // Clean any lingering state then provision test tenant database
        tenantDataSourceCache.evict(TENANT_DB);
        tenantDatabaseService.dropDatabase(TENANT_DB);
        tenantDatabaseService.createDatabase(TENANT_DB);
        tenantFlywayService.migrate(TENANT_DB);
    }

    @AfterEach
    void tearDown() {
        tenantDataSourceCache.evict(TENANT_DB);
        tenantDatabaseService.dropDatabase(TENANT_DB);
        TenantContext.clear();
    }

    @Test
    @DisplayName("AC-1.7.3: Dynamic tenant provisioning, entity persistence, zero-password and hashed-token invariants")
    void testTenantOperationalEntitiesAndInvariants() {
        routingBridge.runInTenant(TENANT_DB, () -> {
            // 1. TenantUser: Verify scalar centralUserId and zero password fields
            TenantUser user = TenantUser.builder()
                    .centralUserId(1001L)
                    .firstName("Alice")
                    .lastName("Smith")
                    .email("alice@testgym.com")
                    .phoneNumber("+15551234")
                    .role(TenantRole.MEMBER)
                    .active(true)
                    .build();

            TenantUser savedUser = tenantUserRepository.saveAndFlush(user);
            assertThat(savedUser.getId()).isNotNull();
            assertThat(savedUser.getCentralUserId()).isEqualTo(1001L);

            Optional<TenantUser> fetchedUser = tenantUserRepository.findByCentralUserId(1001L);
            assertThat(fetchedUser).isPresent();
            assertThat(fetchedUser.get().getEmail()).isEqualTo("alice@testgym.com");

            // 2. GymConfig: Operational settings
            GymConfig config = GymConfig.builder()
                    .latitude(40.7128)
                    .longitude(-74.0060)
                    .radius(100.0)
                    .timeZone("America/New_York")
                    .openingTime(LocalTime.of(6, 0))
                    .closingTime(LocalTime.of(22, 0))
                    .build();

            GymConfig savedConfig = gymConfigRepository.saveAndFlush(config);
            assertThat(savedConfig.getId()).isNotNull();

            // 3. Member: linked to TenantUser
            Member member = Member.builder()
                    .user(savedUser)
                    .gymId("GYM-M-001")
                    .joinDate(LocalDate.now())
                    .independent(true)
                    .type("GENERAL")
                    .build();

            Member savedMember = memberRepository.saveAndFlush(member);
            assertThat(savedMember.getId()).isNotNull();

            // 4. MembershipPlan: Exact monetary precision (BigDecimal scale 2)
            MembershipPlan plan = MembershipPlan.builder()
                    .name("Monthly Unlimited")
                    .basePrice(new BigDecimal("120.00"))
                    .discountValue(new BigDecimal("20.00"))
                    .finalPrice(new BigDecimal("100.00"))
                    .durationDays(30)
                    .active(true)
                    .build();

            MembershipPlan savedPlan = planRepository.saveAndFlush(plan);
            assertThat(savedPlan.getId()).isNotNull();
            assertThat(savedPlan.getBasePrice()).isEqualByComparingTo(new BigDecimal("120.00"));
            assertThat(savedPlan.getFinalPrice()).isEqualByComparingTo(new BigDecimal("100.00"));

            // 5. Attendance: GPS check-in logging
            Attendance attendance = Attendance.builder()
                    .member(savedMember)
                    .date(LocalDate.now())
                    .checkInTime(Instant.now())
                    .status("PRESENT")
                    .autoCheckedOut(false)
                    .build();

            Attendance savedAttendance = attendanceRepository.saveAndFlush(attendance);
            assertThat(savedAttendance.getId()).isNotNull();
            assertThat(savedAttendance.getStatus()).isEqualTo("PRESENT");

            // 6. GymInvitation: SHA-256 hashed token lookup (64 hex characters)
            String sha256TokenHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
            GymInvitation invitation = GymInvitation.builder()
                    .email("trainer.invite@testgym.com")
                    .role(TenantRole.TRAINER)
                    .invitationTokenHash(sha256TokenHash)
                    .status("PENDING")
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .build();

            GymInvitation savedInvite = invitationRepository.saveAndFlush(invitation);
            assertThat(savedInvite.getId()).isNotNull();

            Optional<GymInvitation> fetchedInvite = invitationRepository.findByInvitationTokenHash(sha256TokenHash);
            assertThat(fetchedInvite).isPresent();
            assertThat(fetchedInvite.get().getEmail()).isEqualTo("trainer.invite@testgym.com");

            // 7. Social: FriendRequest and CustomFriend with customName
            TenantUser user2 = tenantUserRepository.saveAndFlush(TenantUser.builder()
                    .centralUserId(1002L)
                    .firstName("Bob")
                    .lastName("Jones")
                    .email("bob@testgym.com")
                    .role(TenantRole.MEMBER)
                    .active(true)
                    .build());

            Member member2 = memberRepository.saveAndFlush(Member.builder()
                    .user(user2)
                    .gymId("GYM-M-002")
                    .joinDate(LocalDate.now())
                    .independent(true)
                    .type("GENERAL")
                    .build());

            FriendRequest friendRequest = friendRequestRepository.saveAndFlush(FriendRequest.builder()
                    .senderMember(savedMember)
                    .receiverMember(member2)
                    .status(FriendRequestStatus.PENDING)
                    .build());
            assertThat(friendRequest.getId()).isNotNull();
            assertThat(friendRequest.getStatus()).isEqualTo(FriendRequestStatus.PENDING);

            CustomFriend customFriend = customFriendRepository.saveAndFlush(CustomFriend.builder()
                    .userMember(savedMember)
                    .friendMember(member2)
                    .customName("Gym Buddy Bob")
                    .build());
            assertThat(customFriend.getId()).isNotNull();
            assertThat(customFriend.getCustomName()).isEqualTo("Gym Buddy Bob");
        });
    }
}
