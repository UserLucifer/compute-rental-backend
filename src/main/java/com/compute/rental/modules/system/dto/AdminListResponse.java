package com.compute.rental.modules.system.dto;

import java.time.LocalDateTime;

public record AdminListResponse(
        Long adminId,
        String userName,
        Integer status,
        String role,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
}
