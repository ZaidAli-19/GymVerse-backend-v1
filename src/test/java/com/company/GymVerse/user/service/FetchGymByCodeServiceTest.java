package com.company.GymVerse.user.service;

import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.models.central.GymSubscription;
import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.enums.GymStatus;
import com.company.GymVerse.models.central.enums.GymSubscriptionStatus;
import com.company.GymVerse.repositories.central.GymSubscriptionRepository;
import com.company.GymVerse.repositories.central.GymTenantRepository;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import com.company.GymVerse.user.mapper.UserMapper;
import com.company.GymVerse.user.mapper.UserMapperImpl;
import com.company.GymVerse.user.request.FetchGymByCodeRequest;
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

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FetchGymByCodeService Unit Tests")
class FetchGymByCodeServiceTest {

    private Map<String, GymTenant> gymTenantsByCode;
    private Map<Long, List<GymSubscription>> subscriptions;
    private GymTenantRepository gymTenantRepository;
    private GymSubscriptionRepository gymSubscriptionRepository;
    private UserAccountRepository userAccountRepository;
    private UserValidations userValidations;
    private UserHelper userHelper;
    private UserMapper userMapper;
    private FetchGymByCodeService service;

    @BeforeEach
    void setUp() {
        gymTenantsByCode = new HashMap<>();
        subscriptions = new HashMap<>();

        gymTenantRepository = (GymTenantRepository) Proxy.newProxyInstance(
                GymTenantRepository.class.getClassLoader(),
                new Class<?>[]{GymTenantRepository.class},
                (proxy, method, args) -> {
                    if ("findByGymCode".equals(method.getName())) {
                        String code = (String) args[0];
                        return Optional.ofNullable(gymTenantsByCode.get(code));
                    }
                    return null;
                }
        );

        gymSubscriptionRepository = (GymSubscriptionRepository) Proxy.newProxyInstance(
                GymSubscriptionRepository.class.getClassLoader(),
                new Class<?>[]{GymSubscriptionRepository.class},
                (proxy, method, args) -> {
                    if ("findByGymTenantIdOrderBySubscribedAtDesc".equals(method.getName())) {
                        Long gymId = (Long) args[0];
                        return subscriptions.getOrDefault(gymId, List.of());
                    }
                    return null;
                }
        );

        userAccountRepository = (UserAccountRepository) Proxy.newProxyInstance(
                UserAccountRepository.class.getClassLoader(),
                new Class<?>[]{UserAccountRepository.class},
                (proxy, method, args) -> null
        );

        userValidations = new UserValidationsImpl(gymTenantRepository);
        userHelper = new UserHelper(userAccountRepository, gymSubscriptionRepository);
        userMapper = new UserMapperImpl();

        service = new FetchGymByCodeService(gymTenantRepository, userValidations, userHelper, userMapper);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", "N/A", List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getRequestType returns FETCH_GYM_BY_CODE")
    void getRequestType_Success() {
        assertThat(service.getRequestType()).isEqualTo(UserRequestType.FETCH_GYM_BY_CODE);
    }

    @Test
    @DisplayName("execute retrieves gym metadata successfully")
    void execute_Success() {
        GymTenant gym = GymTenant.builder()
                .id(101L)
                .gymCode("IRON-99")
                .gymName("Iron Paradise")
                .address("77 Muscle Beach")
                .city("Venice")
                .status(GymStatus.ACTIVE)
                .build();
        gymTenantsByCode.put("IRON-99", gym);

        subscriptions.put(101L, List.of(GymSubscription.builder().status(GymSubscriptionStatus.ACTIVE).build()));

        UserRequest request = UserRequest.builder()
                .requestType(UserRequestType.FETCH_GYM_BY_CODE)
                .fetchGymByCodeRequest(FetchGymByCodeRequest.builder().gymCode("iron-99").build())
                .build();

        UserResponse response = service.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getFetchGymByCodeResponse()).isNotNull();
        assertThat(response.getFetchGymByCodeResponse().getGymCode()).isEqualTo("IRON-99");
        assertThat(response.getFetchGymByCodeResponse().getGymName()).isEqualTo("Iron Paradise");
        assertThat(response.getFetchGymByCodeResponse().getSubscriptionStatus()).isEqualTo(GymSubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("execute throws ResourceNotFoundException on unknown gym code")
    void execute_NotFound() {
        UserRequest request = UserRequest.builder()
                .requestType(UserRequestType.FETCH_GYM_BY_CODE)
                .fetchGymByCodeRequest(FetchGymByCodeRequest.builder().gymCode("UNKNOWN").build())
                .build();

        assertThatThrownBy(() -> service.execute(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Gym not found with code: UNKNOWN");
    }

    @Test
    @DisplayName("execute throws BadRequestException on suspended gym")
    void execute_SuspendedGym() {
        GymTenant gym = GymTenant.builder()
                .id(102L)
                .gymCode("OFF-10")
                .status(GymStatus.SUSPENDED)
                .build();
        gymTenantsByCode.put("OFF-10", gym);

        UserRequest request = UserRequest.builder()
                .requestType(UserRequestType.FETCH_GYM_BY_CODE)
                .fetchGymByCodeRequest(FetchGymByCodeRequest.builder().gymCode("OFF-10").build())
                .build();

        assertThatThrownBy(() -> service.execute(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is currently SUSPENDED");
    }
}
