package com.compute.rental.modules.commission.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record TeamMemberResponse(
        String userId,
        @Schema(description = "用户名称")
        String userName,
        String email,
        Integer status,
        Integer levelDepth,
        LocalDateTime createdAt
) {
}
