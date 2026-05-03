package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AdminTeamRelationResponse(
        @Schema(description = "Team relation internal ID")
        Long id,
        @Schema(description = "Ancestor user internal ID")
        Long ancestorUserId,
        @Schema(description = "Ancestor user name")
        String ancestorUserName,
        @Schema(description = "Descendant user internal ID")
        Long descendantUserId,
        @Schema(description = "Descendant user name")
        String descendantUserName,
        @Schema(description = "Level depth")
        Integer levelDepth,
        @Schema(description = "Created time")
        LocalDateTime createdAt
) {
}
