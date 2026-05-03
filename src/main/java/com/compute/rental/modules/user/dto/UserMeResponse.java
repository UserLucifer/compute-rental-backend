package com.compute.rental.modules.user.dto;

import java.time.LocalDateTime;

public record UserMeResponse(
        Long id,
        String userId,
        String email,
        String userName,
        String avatarKey,
        Integer status,
        LocalDateTime createdAt
) {
}
