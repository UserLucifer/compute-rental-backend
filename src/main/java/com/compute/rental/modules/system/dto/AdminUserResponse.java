package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AdminUserResponse(
        @Schema(description = "User internal ID")
        Long id,
        @Schema(description = "User public ID")
        String userId,
        @Schema(description = "Email")
        String email,
        @Schema(description = "User name")
        String userName,
        @Schema(description = "Avatar key")
        String avatarKey,
        @Schema(description = "User status")
        Integer status,
        @Schema(description = "Email verified time")
        LocalDateTime emailVerifiedAt,
        @Schema(description = "Last login time")
        LocalDateTime lastLoginAt,
        @Schema(description = "Created time")
        LocalDateTime createdAt,
        @Schema(description = "Updated time")
        LocalDateTime updatedAt
) {
}
