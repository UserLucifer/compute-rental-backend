package com.compute.rental.modules.blog.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.modules.blog.dto.BlogCategoryResponse;
import com.compute.rental.modules.blog.dto.BlogPostResponse;
import com.compute.rental.modules.blog.dto.BlogTagResponse;
import com.compute.rental.modules.blog.service.BlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Blog")
@RestController
@RequestMapping("/api/blog")
public class BlogController {

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @Operation(summary = "Public blog categories")
    @GetMapping("/categories")
    public ApiResponse<List<BlogCategoryResponse>> categories() {
        return ApiResponse.success(blogService.publicCategories());
    }

    @Operation(summary = "Public blog tags")
    @GetMapping("/tags")
    public ApiResponse<List<BlogTagResponse>> tags() {
        return ApiResponse.success(blogService.publicTags());
    }

    @Operation(summary = "Public blog posts")
    @GetMapping("/posts")
    public ApiResponse<PageResult<BlogPostResponse>> posts(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false, name = "category_id") Long categoryId,
            @RequestParam(required = false, name = "tag_id") Long tagId,
            @RequestParam(required = false, name = "start_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false, name = "end_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        return ApiResponse.success(blogService.publicPosts(pageNo, pageSize, categoryId, tagId, startTime, endTime));
    }

    @Operation(summary = "Public blog post detail")
    @GetMapping("/posts/{id}")
    public ApiResponse<BlogPostResponse> post(@PathVariable Long id) {
        return ApiResponse.success(blogService.publicPost(id));
    }
}
