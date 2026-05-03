package com.compute.rental.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        @Pattern(regexp = "^\\d{4,10}$", message = "验证码必须为数字")
        String code,

        @NotBlank
        @Size(max = 72)
        String newPassword
) {
}
