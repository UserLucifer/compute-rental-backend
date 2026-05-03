package com.compute.rental.modules.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UnregisterPushDeviceRequest(
        @NotBlank String deviceToken
) {
}
