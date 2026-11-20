package com.company.GymVerse.user.validations;

import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.models.central.GymTenant;
import com.company.GymVerse.models.central.enums.GymStatus;
import com.company.GymVerse.models.central.enums.GymSubscriptionStatus;
import com.company.GymVerse.repositories.central.GymTenantRepository;
import com.company.GymVerse.user.request.FetchGymByCodeRequest;
import com.company.GymVerse.user.request.RegisterGymRequest;
import com.company.GymVerse.user.request.UserRequest;
import com.company.GymVerse.user.request.enums.UserRequestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("User Validations Unit Tests")
class UserValidationsTest {

    private Set<String> existingGymCodes;
    private GymTenantRepository gymTenantRepository;
    private UserValidations userValidations;

    @BeforeEach
    void setUp() {
        existingGymCodes = new HashSet<>();

        gymTenantRepository = (GymTenantRepository) Proxy.newProxyInstance(
                GymTenantRepository.class.getClassLoader(),
                new Class<?>[]{GymTenantRepository.class},
                (proxy, method, args) -> {
                    if ("findByGymCode".equals(method.getName())) {
                        String code = (String) args[0];
                        if (existingGymCodes.contains(code)) {
                            return Optional.of(GymTenant.builder().gymCode(code).build());
                        }
                        return Optional.empty();
                    }
                    return null;
                }
        );

        userValidations = new UserValidationsImpl(gymTenantRepository);
    }

    private UserRequest buildValidRegisterRequest() {
        return UserRequest.builder()
                .requestType(UserRequestType.REGISTER_GYM)
                .registerGymRequest(RegisterGymRequest.builder()
                        .gymName("Metro Flex")
                        .address("123 Main St")
                        .city("Dallas")
                        .build())
                .build();
    }

    @Test
    @DisplayName("validateRegisterGym succeeds for valid request")
    void validateRegisterGym_Success() {
        UserRequest request = buildValidRegisterRequest();
        assertThatCode(() -> userValidations.validateRegisterGym(request))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateRegisterGym throws BadRequestException on null request or payload")
    void validateRegisterGym_NullGuards() {
        assertThatThrownBy(() -> userValidations.validateRegisterGym(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("User request must not be null");

        UserRequest reqNoPayload = UserRequest.builder().requestType(UserRequestType.REGISTER_GYM).build();
        assertThatThrownBy(() -> userValidations.validateRegisterGym(reqNoPayload))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Register gym request payload must not be null");
    }

    @Test
    @DisplayName("validateRegisterGym throws BadRequestException on missing required fields")
    void validateRegisterGym_MissingFields() {
        UserRequest reqNoName = buildValidRegisterRequest();
        reqNoName.getRegisterGymRequest().setGymName("  ");
        assertThatThrownBy(() -> userValidations.validateRegisterGym(reqNoName))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Gym name is required");

        UserRequest reqNoAddress = buildValidRegisterRequest();
        reqNoAddress.getRegisterGymRequest().setAddress("");
        assertThatThrownBy(() -> userValidations.validateRegisterGym(reqNoAddress))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Gym address is required");

        UserRequest reqNoCity = buildValidRegisterRequest();
        reqNoCity.getRegisterGymRequest().setCity(null);
        assertThatThrownBy(() -> userValidations.validateRegisterGym(reqNoCity))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Gym city is required");
    }

    @Test
    @DisplayName("validateRegisterGym validates latitude and longitude boundaries")
    void validateRegisterGym_CoordinateBoundaries() {
        UserRequest reqBadLat = buildValidRegisterRequest();
        reqBadLat.getRegisterGymRequest().setLatitude(BigDecimal.valueOf(95));
        assertThatThrownBy(() -> userValidations.validateRegisterGym(reqBadLat))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Latitude must be between -90 and 90");

        UserRequest reqBadLng = buildValidRegisterRequest();
        reqBadLng.getRegisterGymRequest().setLongitude(BigDecimal.valueOf(-190));
        assertThatThrownBy(() -> userValidations.validateRegisterGym(reqBadLng))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Longitude must be between -180 and 180");
    }

    @Test
    @DisplayName("validateCustomGymCode validates format and uniqueness")
    void validateCustomGymCode_FormatAndUniqueness() {
        assertThatCode(() -> userValidations.validateCustomGymCode("IRON99"))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> userValidations.validateCustomGymCode("A"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Custom gym code must be 4 to 10 alphanumeric characters");

        existingGymCodes.add("TAKEN");
        assertThatThrownBy(() -> userValidations.validateCustomGymCode("TAKEN"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("validateFetchGymByCode validates null and blank codes")
    void validateFetchGymByCode_Checks() {
        assertThatThrownBy(() -> userValidations.validateFetchGymByCode(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("User request must not be null");

        UserRequest emptyEnvelope = UserRequest.builder().requestType(UserRequestType.FETCH_GYM_BY_CODE).build();
        assertThatThrownBy(() -> userValidations.validateFetchGymByCode(emptyEnvelope))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Fetch gym by code request payload must not be null");

        UserRequest blankCodeReq = UserRequest.builder()
                .requestType(UserRequestType.FETCH_GYM_BY_CODE)
                .fetchGymByCodeRequest(FetchGymByCodeRequest.builder().gymCode("   ").build())
                .build();
        assertThatThrownBy(() -> userValidations.validateFetchGymByCode(blankCodeReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Gym code must not be null or blank");

        UserRequest validReq = UserRequest.builder()
                .requestType(UserRequestType.FETCH_GYM_BY_CODE)
                .fetchGymByCodeRequest(FetchGymByCodeRequest.builder().gymCode("FIT-101").build())
                .build();
        assertThatCode(() -> userValidations.validateFetchGymByCode(validReq))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateGymActiveForJoin validates gym status and subscription health")
    void validateGymActiveForJoin_Checks() {
        GymTenant suspendedGym = GymTenant.builder().gymCode("GYM-99").status(GymStatus.SUSPENDED).build();
        assertThatThrownBy(() -> userValidations.validateGymActiveForJoin(suspendedGym, GymSubscriptionStatus.ACTIVE))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is currently SUSPENDED");

        GymTenant activeGym = GymTenant.builder().gymCode("GYM-10").status(GymStatus.ACTIVE).build();
        assertThatThrownBy(() -> userValidations.validateGymActiveForJoin(activeGym, GymSubscriptionStatus.EXPIRED))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("subscription is inactive");

        assertThatCode(() -> userValidations.validateGymActiveForJoin(activeGym, GymSubscriptionStatus.ACTIVE))
                .doesNotThrowAnyException();

        assertThatCode(() -> userValidations.validateGymActiveForJoin(activeGym, GymSubscriptionStatus.TRIAL))
                .doesNotThrowAnyException();
    }
}
