package com.company.GymVerse.user.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for discovering and retrieving gym metadata by its discovery code.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FetchGymByCodeRequest {

    private String gymCode;
}
