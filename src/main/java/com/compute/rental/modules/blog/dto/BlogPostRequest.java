package com.compute.rental.modules.blog.dto;

import java.util.List;

public record BlogPostRequest(
        Long categoryId,
        String title,
        String summary,
        String coverImageUrl,
        String contentMarkdown,
        Integer publishStatus,
        Integer isTop,
        Integer sortNo,
        List<Long> tagIds
) {
}
