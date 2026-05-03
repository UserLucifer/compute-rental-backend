package com.compute.rental.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendEmailCodeRequest(
        @NotBlank
        @Email
        String email
) {
}
