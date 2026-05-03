package com.compute.rental.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record RedisCacheClearResponse(
        @Schema(description = "Deleted Redis key count")
        long deletedCount,
        @Schema(description = "Cleared key prefixes")
        List<String> prefixes,
        @Schema(description = "Execution time")
        LocalDateTime executedAt
) {
}
