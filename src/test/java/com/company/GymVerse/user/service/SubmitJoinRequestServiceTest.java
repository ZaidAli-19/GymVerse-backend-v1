package com.company.GymVerse.user.service;

import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.models.central.GymSubscription;
import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.UserTenantRole;
import com.company.GymVerse.models.central.enums.GymStatus;
import com.company.GymVerse.models.central.enums.GymSubscriptionStatus;
import com.company.GymVerse.models.central.enums.TenantMembershipStatus;
import com.company.GymVerse.models.central.enums.TenantRole;
import com.company.GymVerse.models.tenant.JoinRequest;
import com.company.GymVerse.models.tenant.enums.JoinRequestStatus;
import com.company.GymVerse.multitanancy.TenantRoutingBridge;
import com.company.GymVerse.repositories.central.GymSubscriptionRepository;
import com.company.GymVerse.repositories.central.GymTenantRepository;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import com.company.GymVerse.repositories.central.UserTenantRoleRepository;
import com.company.GymVerse.repositories.tenant.JoinRequestRepository;
import com.company.GymVerse.user.mapper.UserMapper;
import com.company.GymVerse.user.mapper.UserMapperImpl;
import com.company.GymVerse.user.request.SubmitJoinRequest;
import com.company.GymVerse.user.request.UserRequest;
import com.company.GymVerse.user.request.enums.UserRequestType;
import com.company.GymVerse.user.response.UserResponse;
import com.company.GymVerse.user.util.UserHelper;
import com.company.GymVerse.user.validations.UserValidations;
import com.company.GymVerse.user.validations.UserValidationsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionCallback;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SubmitJoinRequestService Unit Tests")
class SubmitJoinRequestServiceTest {

    private Map<String, GymTenant> gymTenants;
    private Map<Long, UserAccount> userAccounts;
    private Map<String, UserTenantRole> userTenantRoles;
    private List<JoinRequest> savedJoinRequests;

    private GymTenantRepository gymTenantRepository;
    private JoinRequestRepository joinRequestRepository;
    private TenantRoutingBridge tenantRoutingBridge;
    private GymSubscriptionRepository gymSubscriptionRepository;
    private UserAccountRepository userAccountRepository;
    private UserTenantRoleRepository userTenantRoleRepository;

    private UserValidations userValidations;
    private UserHelper userHelper;
    private UserMapper userMapper;
    private SubmitJoinRequestService service;

    @BeforeEach
    void setUp() {
        gymTenants = new HashMap<>();
        userAccounts = new HashMap<>();
        userTenantRoles = new HashMap<>();
        savedJoinRequests = new ArrayList<>();

        gymTenantRepository = (GymTenantRepository) Proxy.newProxyInstance(
                GymTenantRepository.class.getClassLoader(),
                new Class<?>[]{GymTenantRepository.class},
                (proxy, method, args) -> {
                    if ("findByGymCode".equals(method.getName())) {
                        String code = (String) args[0];
                        return Optional.ofNullable(gymTenants.get(code));
                    }
                    return null;
                }
        );

        userAccountRepository = (UserAccountRepository) Proxy.newProxyInstance(
                UserAccountRepository.class.getClassLoader(),
                new Class<?>[]{UserAccountRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        Long id = (Long) args[0];
                        return Optional.ofNullable(userAccounts.get(id));
                    }
                    return null;
                }
        );

        userTenantRoleRepository = (UserTenantRoleRepository) Proxy.newProxyInstance(
                UserTenantRoleRepository.class.getClassLoader(),
                new Class<?>[]{UserTenantRoleRepository.class},
                (proxy, method, args) -> {
                    if ("findByUserAccountIdAndGymTenantId".equals(method.getName())) {
                        Long uid = (Long) args[0];
                        Long gid = (Long) args[1];
                        return Optional.ofNullable(userTenantRoles.get(uid + "-" + gid));
                    }
                    return null;
                }
        );

        gymSubscriptionRepository = (GymSubscriptionRepository) Proxy.newProxyInstance(
                GymSubscriptionRepository.class.getClassLoader(),
                new Class<?>[]{GymSubscriptionRepository.class},
                (proxy, method, args) -> {
                    if ("findByGymTenantIdOrderBySubscribedAtDesc".equals(method.getName())) {
                        return List.of(GymSubscription.builder().status(GymSubscriptionStatus.ACTIVE).build());
                    }
                    return null;
                }
        );

        joinRequestRepository = (JoinRequestRepository) Proxy.newProxyInstance(
                JoinRequestRepository.class.getClassLoader(),
                new Class<?>[]{JoinRequestRepository.class},
                (proxy, method, args) -> {
                    if ("existsByCentralUserIdAndStatus".equals(method.getName())) {
                        Long uid = (Long) args[0];
                        JoinRequestStatus st = (JoinRequestStatus) args[1];
                        return savedJoinRequests.stream()
                                .anyMatch(r -> r.getCentralUserId().equals(uid) && r.getStatus() == st);
                    }
                    if ("save".equals(method.getName())) {
                        JoinRequest req = (JoinRequest) args[0];
                        req.setId((long) (savedJoinRequests.size() + 1));
                        savedJoinRequests.add(req);
                        return req;
                    }
                    return null;
                }
        );

        tenantRoutingBridge = new TenantRoutingBridge(null) {
            @Override
            public <T> T callInTenantTx(String tenantDbName, TransactionCallback<T> action) {
                return action.doInTransaction(null);
            }
        };

        userValidations = new UserValidationsImpl(gymTenantRepository, userTenantRoleRepository);
        userHelper = new UserHelper(userAccountRepository, gymSubscriptionRepository);
        userMapper = new UserMapperImpl();

        service = new SubmitJoinRequestService(
                gymTenantRepository,
                joinRequestRepository,
                tenantRoutingBridge,
                userValidations,
                userHelper,
                userMapper
        );

        UserAccount caller = UserAccount.builder()
                .id(1L)
                .email("caller@gym.com")
                .firstName("Alex")
                .lastName("Smith")
                .phoneNumber("555-0100")
                .build();
        userAccounts.put(1L, caller);

        GymTenant gym = GymTenant.builder()
                .id(100L)
                .gymCode("GOLD-10")
                .gymName("Gold Gym")
                .dbName("gymverse_tenant_gold10")
                .status(GymStatus.ACTIVE)
                .build();
        gymTenants.put("GOLD-10", gym);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", "N/A", List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getRequestType returns SUBMIT_JOIN_REQUEST")
    void getRequestType_Success() {
        assertThat(service.getRequestType()).isEqualTo(UserRequestType.SUBMIT_JOIN_REQUEST);
    }

    @Test
    @DisplayName("execute successfully creates join request in tenant DB")
    void execute_Success() {
        UserRequest request = UserRequest.builder()
                .requestType(UserRequestType.SUBMIT_JOIN_REQUEST)
                .submitJoinRequest(SubmitJoinRequest.builder()
                        .gymCode("gold-10")
                        .requestNotes("Want to join weightlifting classes")
                        .build())
                .build();

        UserResponse response = service.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getSubmitJoinRequestResponse()).isNotNull();
        assertThat(response.getSubmitJoinRequestResponse().getGymCode()).isEqualTo("GOLD-10");
        assertThat(response.getSubmitJoinRequestResponse().getStatus()).isEqualTo(JoinRequestStatus.PENDING);
        assertThat(response.getSubmitJoinRequestResponse().getRequestNotes()).isEqualTo("Want to join weightlifting classes");
        assertThat(savedJoinRequests).hasSize(1);
        assertThat(savedJoinRequests.get(0).getCentralUserId()).isEqualTo(1L);
        assertThat(savedJoinRequests.get(0).getEmail()).isEqualTo("caller@gym.com");
    }

    @Test
    @DisplayName("execute throws BadRequestException if user is already an active member")
    void execute_AlreadyMember() {
        userTenantRoles.put("1-100", UserTenantRole.builder()
                .role(TenantRole.MEMBER)
                .status(TenantMembershipStatus.ACTIVE)
                .build());

        UserRequest request = UserRequest.builder()
                .requestType(UserRequestType.SUBMIT_JOIN_REQUEST)
                .submitJoinRequest(SubmitJoinRequest.builder().gymCode("GOLD-10").build())
                .build();

        assertThatThrownBy(() -> service.execute(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("User is already an active MEMBER");
    }

    @Test
    @DisplayName("execute throws BadRequestException if pending join request already exists")
    void execute_PendingAlreadyExists() {
        savedJoinRequests.add(JoinRequest.builder()
                .id(99L)
                .centralUserId(1L)
                .status(JoinRequestStatus.PENDING)
                .build());

        UserRequest request = UserRequest.builder()
                .requestType(UserRequestType.SUBMIT_JOIN_REQUEST)
                .submitJoinRequest(SubmitJoinRequest.builder().gymCode("GOLD-10").build())
                .build();

        assertThatThrownBy(() -> service.execute(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("A pending join request already exists");
    }

    @Test
    @DisplayName("execute throws ResourceNotFoundException on unknown gym code")
    void execute_UnknownGym() {
        UserRequest request = UserRequest.builder()
                .requestType(UserRequestType.SUBMIT_JOIN_REQUEST)
                .submitJoinRequest(SubmitJoinRequest.builder().gymCode("NOPE-00").build())
                .build();

        assertThatThrownBy(() -> service.execute(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Gym not found with code: NOPE-00");
    }
}
