package com.company.GymVerse.user.request;

import com.company.GymVerse.user.request.enums.UserRequestType;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified request envelope for all central master user operations.
 * Exclusively driven by the {@link UserRequestType} discriminator.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {

    @JsonAlias({"userRequestType", "requestType"})
    private UserRequestType requestType;

    @JsonAlias({"registerGym", "registerGymRequest"})
    private RegisterGymRequest registerGymRequest;

    @JsonAlias({"fetchMyTenants", "fetchMyTenantsRequest"})
    private FetchMyTenantsRequest fetchMyTenantsRequest;

    @JsonAlias({"fetchGymByCode", "fetchGymByCodeRequest", "discoverGym"})
    private FetchGymByCodeRequest fetchGymByCodeRequest;
}
