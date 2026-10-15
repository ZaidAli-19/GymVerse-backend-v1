package com.company.GymVerse.platformAdmin.request;

import com.company.GymVerse.models.central.enums.GymStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Filter criteria and pagination parameters for querying registered gym tenants.
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
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}
