package com.compute.rental.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAvatarRequest(
        @NotBlank
        @Size(max = 64)
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "avatarKey only supports letters, numbers, underscore and hyphen")
        String avatarKey
) {
}
