package com.compute.rental.modules.system.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(
        @NotBlank
        String userName,

        @NotBlank
        String password
) {
}
