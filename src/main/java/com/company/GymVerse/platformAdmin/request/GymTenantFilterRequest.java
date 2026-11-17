package com.company.GymVerse.platformAdmin.request;

import com.company.GymVerse.models.central.enums.GymStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Filter criteria for querying registered gym tenants.
 * Pagination and sorting are managed exclusively at the root PlatformAdminRequest level.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GymTenantFilterRequest {

    private GymStatus status;
    private String city;
    private String gymName;
    private String gymCode;
}
