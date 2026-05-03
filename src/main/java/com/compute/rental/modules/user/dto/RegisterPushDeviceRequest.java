package com.compute.rental.modules.user.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterPushDeviceRequest(
        @NotBlank String deviceType,
        @NotBlank String deviceToken
) {
}
