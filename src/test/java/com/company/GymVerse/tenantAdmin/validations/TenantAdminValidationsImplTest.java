package com.company.GymVerse.tenantAdmin.validations;

import com.company.GymVerse.exceptions.common.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantAdminValidationsImplTest {

    private final TenantAdminValidationsImpl validations = new TenantAdminValidationsImpl();
    private final Set<String> allowedFields = Set.of("id", "name", "basePrice");

    @Test
    @DisplayName("Builds default Pageable when optional pagination fields are null")
    void validateAndBuildPageable_Defaults() {
        Pageable pageable = validations.validateAndBuildPageable(null, null, null, null, "id", allowedFields);
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("id")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("Rejects invalid page, size, sort field, and sort direction")
    void validateAndBuildPageable_InvalidInputs() {
        assertThatThrownBy(() -> validations.validateAndBuildPageable(-1, 10, "id", "asc", "id", allowedFields))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> validations.validateAndBuildPageable(0, 101, "id", "asc", "id", allowedFields))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> validations.validateAndBuildPageable(0, 10, "secretField", "asc", "id", allowedFields))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> validations.validateAndBuildPageable(0, 10, "id", "sideways", "id", allowedFields))
                .isInstanceOf(BadRequestException.class);
    }
}
