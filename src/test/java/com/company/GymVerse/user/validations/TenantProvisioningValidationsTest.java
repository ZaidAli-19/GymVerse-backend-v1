package com.company.GymVerse.user.validations;

import com.company.GymVerse.exceptions.common.BadRequestException;
import com.company.GymVerse.models.central.UserAccount;
import com.company.GymVerse.repositories.central.GymTenantRepository;
import com.company.GymVerse.user.dto.TenantProvisioningCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Tenant Provisioning Validations Tests")
class TenantProvisioningValidationsTest {

    private Set<String> existingGymCodes;
    private GymTenantRepository gymTenantRepository;
    private TenantProvisioningValidations validations;
    private UserAccount mockUserAccount;

    @BeforeEach
    void setUp() {
        existingGymCodes = new HashSet<>();

        gymTenantRepository = (GymTenantRepository) Proxy.newProxyInstance(
                GymTenantRepository.class.getClassLoader(),
                new Class<?>[]{GymTenantRepository.class},
                (proxy, method, args) -> {
                    if ("existsByGymCode".equals(method.getName())) {
                        return existingGymCodes.contains(args[0]);
                    }
                    return null;
                }
        );

        validations = new TenantProvisioningValidationsImpl(gymTenantRepository);

        mockUserAccount = UserAccount.builder()
                .id(1L)
                .email("admin@gym.com")
                .firstName("Admin")
                .lastName("User")
                .build();
    }

    private TenantProvisioningCommand buildValidCommand() {
        return TenantProvisioningCommand.builder()
                .userAccount(mockUserAccount)
                .gymName("Metro Flex Gym")
                .address("123 Main St")
                .city("Dallas")
                .build();
    }

    @Test
    @DisplayName("Valid provisioning command succeeds without throwing exceptions")
    void validateProvisioningCommand_Success() {
        TenantProvisioningCommand command = buildValidCommand();
        assertThatCode(() -> validations.validateProvisioningCommand(command))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Valid command with unique custom gym code succeeds")
    void validateProvisioningCommand_Success_WithCustomGymCode() {
        TenantProvisioningCommand command = buildValidCommand();
        command.setCustomGymCode("METRO-DALLAS-01");

        assertThatCode(() -> validations.validateProvisioningCommand(command))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Throws BadRequestException when command is null")
    void validateProvisioningCommand_NullCommand() {
        assertThatThrownBy(() -> validations.validateProvisioningCommand(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Tenant provisioning command must not be null");
    }

    @Test
    @DisplayName("Throws BadRequestException when user account is null")
    void validateProvisioningCommand_NullUserAccount() {
        TenantProvisioningCommand command = buildValidCommand();
        command.setUserAccount(null);

        assertThatThrownBy(() -> validations.validateProvisioningCommand(command))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("UserAccount with a non-null ID is required");
    }

    @Test
    @DisplayName("Throws BadRequestException when user account has null ID")
    void validateProvisioningCommand_UserAccountNullId() {
        TenantProvisioningCommand command = buildValidCommand();
        command.setUserAccount(UserAccount.builder().email("test@gym.com").build());

        assertThatThrownBy(() -> validations.validateProvisioningCommand(command))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("UserAccount with a non-null ID is required");
    }

    @Test
    @DisplayName("Throws BadRequestException when gym name is blank")
    void validateProvisioningCommand_BlankGymName() {
        TenantProvisioningCommand command = buildValidCommand();
        command.setGymName("   ");

        assertThatThrownBy(() -> validations.validateProvisioningCommand(command))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Gym name is required");
    }

    @Test
    @DisplayName("Throws BadRequestException when address is blank")
    void validateProvisioningCommand_BlankAddress() {
        TenantProvisioningCommand command = buildValidCommand();
        command.setAddress("");

        assertThatThrownBy(() -> validations.validateProvisioningCommand(command))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Gym address is required");
    }

    @Test
    @DisplayName("Throws BadRequestException when city is blank")
    void validateProvisioningCommand_BlankCity() {
        TenantProvisioningCommand command = buildValidCommand();
        command.setCity(" ");

        assertThatThrownBy(() -> validations.validateProvisioningCommand(command))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Gym city is required");
    }

    @Test
    @DisplayName("Throws BadRequestException when custom gym code already exists")
    void validateCustomGymCode_Collision() {
        existingGymCodes.add("EXISTING-CODE");

        assertThatThrownBy(() -> validations.validateCustomGymCode("EXISTING-CODE"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Gym code 'EXISTING-CODE' is already taken");
    }

    @Test
    @DisplayName("Custom gym code validation ignores null and blank strings")
    void validateCustomGymCode_NullOrBlankIgnored() {
        assertThatCode(() -> validations.validateCustomGymCode(null)).doesNotThrowAnyException();
        assertThatCode(() -> validations.validateCustomGymCode("")).doesNotThrowAnyException();
        assertThatCode(() -> validations.validateCustomGymCode("   ")).doesNotThrowAnyException();
    }
}
