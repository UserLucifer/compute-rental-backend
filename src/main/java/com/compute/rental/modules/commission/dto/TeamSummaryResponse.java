package com.compute.rental.modules.commission.dto;

public record TeamSummaryResponse(
        long totalTeamCount,
        long directTeamCount,
        long level2TeamCount,
        long level3TeamCount,
        long deeperTeamCount
) {
}
