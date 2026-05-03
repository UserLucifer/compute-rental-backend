package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AdminUserTeamResponse(
        @Schema(description = "User internal ID")
        Long userId,
        @Schema(description = "Total team count")
        long totalTeamCount,
        @Schema(description = "Direct team count")
        long directTeamCount,
        @Schema(description = "Level 2 team count")
        long level2TeamCount,
        @Schema(description = "Level 3 team count")
        long level3TeamCount,
        @Schema(description = "Deeper team count")
        long deeperTeamCount,
        @Schema(description = "Team relations")
        List<AdminTeamRelationResponse> relations
) {
}
