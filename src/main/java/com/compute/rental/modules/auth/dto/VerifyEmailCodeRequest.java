package com.compute.rental.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailCodeRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        @Pattern(regexp = "^\\d{4,10}$", message = "验证码必须为数字")
        String code
) {
}
