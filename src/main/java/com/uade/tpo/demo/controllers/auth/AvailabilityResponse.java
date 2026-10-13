package com.uade.tpo.demo.controllers.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// null = no se consultó ese dato (no vino en la query)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AvailabilityResponse {

    private Boolean usernameAvailable;
    private Boolean emailAvailable;
}
