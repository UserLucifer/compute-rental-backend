package com.compute.rental.modules.commission.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record TeamMemberResponse(
        String userId,
        @Schema(description = "用户名称")
        String userName,
        String avatarKey,
        Integer status,
        Integer levelDepth,
        LocalDateTime createdAt,
        @Schema(description = "该成员的直接和间接下级总数")
        Long subTeamCount,
        @Schema(description = "直接上级公开用户 ID")
        String parentId
) {
}
