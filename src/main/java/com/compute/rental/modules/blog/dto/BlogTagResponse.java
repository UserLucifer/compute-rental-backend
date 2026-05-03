package com.compute.rental.modules.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record BlogTagResponse(
        @Schema(description = "Tag internal ID")
        Long id,
        @Schema(description = "Tag name")
        String tagName,
        @Schema(description = "Sort number")
        Integer sortNo,
        @Schema(description = "Tag status")
        Integer status,
        @Schema(description = "Created time")
        LocalDateTime createdAt,
        @Schema(description = "Updated time")
        LocalDateTime updatedAt
) {
}
