package com.company.GymVerse.user.util;

import com.company.GymVerse.exceptions.common.ResourceNotFoundException;
import com.company.GymVerse.models.central.GymSubscription;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.models.central.enums.GymSubscriptionStatus;
import com.company.GymVerse.repositories.central.GymSubscriptionRepository;
import com.company.GymVerse.repositories.central.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserHelper Unit Tests")
class UserHelperTest {

    private Map<Long, UserAccount> userAccounts;
    private Map<Long, List<GymSubscription>> subscriptions;
    private UserAccountRepository userAccountRepository;
    private GymSubscriptionRepository gymSubscriptionRepository;
    private UserHelper userHelper;

    @BeforeEach
    void setUp() {
        userAccounts = new HashMap<>();
        subscriptions = new HashMap<>();

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

        userHelper = new UserHelper(userAccountRepository, gymSubscriptionRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getAuthenticatedUserAccount resolves user successfully")
    void getAuthenticatedUserAccount_Success() {
        UserAccount account = UserAccount.builder()
                .id(42L)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();
        userAccounts.put(42L, account);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("42", "N/A")
        );

        UserAccount resolved = userHelper.getAuthenticatedUserAccount();
        assertThat(resolved).isNotNull();
        assertThat(resolved.getId()).isEqualTo(42L);
        assertThat(resolved.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("getAuthenticatedUserAccount throws ResourceNotFoundException on missing account")
    void getAuthenticatedUserAccount_NotFound() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("999", "N/A")
        );

        assertThatThrownBy(() -> userHelper.getAuthenticatedUserAccount())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UserAccount not found");
    }

    @Test
    @DisplayName("getAuthenticatedUserAccount throws ResourceNotFoundException when unauthenticated")
    void getAuthenticatedUserAccount_NoAuth() {
        assertThatThrownBy(() -> userHelper.getAuthenticatedUserAccount())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No authenticated security context found");
    }

    @Test
    @DisplayName("normalizeGymCode trims and uppercases codes")
    void normalizeGymCode_Transforms() {
        assertThat(userHelper.normalizeGymCode("  iron-gym-1  ")).isEqualTo("IRON-GYM-1");
        assertThat(userHelper.normalizeGymCode(null)).isNull();
    }

    @Test
    @DisplayName("getLatestSubscriptionStatus returns latest status or null")
    void getLatestSubscriptionStatus_Retrieves() {
        assertThat(userHelper.getLatestSubscriptionStatus(null)).isNull();
        assertThat(userHelper.getLatestSubscriptionStatus(100L)).isNull();

        List<GymSubscription> list = new ArrayList<>();
        list.add(GymSubscription.builder().status(GymSubscriptionStatus.ACTIVE).build());
        subscriptions.put(100L, list);

        assertThat(userHelper.getLatestSubscriptionStatus(100L)).isEqualTo(GymSubscriptionStatus.ACTIVE);
    }
}
