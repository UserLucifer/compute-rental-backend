package com.compute.rental.modules.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record BlogTagRequest(
        @Schema(description = "Tag name")
        @NotBlank
        String tagName,
        @Schema(description = "Sort number")
        Integer sortNo,
        @Schema(description = "Tag status")
        Integer status
) {
}
