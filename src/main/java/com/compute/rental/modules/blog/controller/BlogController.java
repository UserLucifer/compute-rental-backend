package com.compute.rental.modules.blog.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.i18n.LanguageResolver;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.modules.blog.dto.BlogCategoryResponse;
import com.compute.rental.modules.blog.dto.BlogPostResponse;
import com.compute.rental.modules.blog.dto.BlogTagResponse;
import com.compute.rental.modules.blog.service.BlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Blog")
@RestController
@RequestMapping("/api/blog")
public class BlogController {

    private final BlogService blogService;
    private final LanguageResolver languageResolver;

    public BlogController(BlogService blogService, LanguageResolver languageResolver) {
        this.blogService = blogService;
        this.languageResolver = languageResolver;
    }

    @Operation(summary = "Public blog categories")
    @GetMapping("/categories")
    public ApiResponse<List<BlogCategoryResponse>> categories(
            @RequestParam(required = false) String language,
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        addLanguageVary(response);
        var locale = languageResolver.resolve(language, acceptLanguage);
        return ApiResponse.success(blogService.publicCategories(locale));
    }

    @Operation(summary = "Public blog tags")
    @GetMapping("/tags")
    public ApiResponse<List<BlogTagResponse>> tags(
            @RequestParam(required = false) String language,
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        addLanguageVary(response);
        var locale = languageResolver.resolve(language, acceptLanguage);
        return ApiResponse.success(blogService.publicTags(locale));
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
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) String language,
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        addLanguageVary(response);
        var locale = languageResolver.resolve(language, acceptLanguage);
        return ApiResponse.success(blogService.publicPosts(pageNo, pageSize, categoryId, tagId, startTime, endTime, locale));
    }

    @Operation(summary = "Public blog post detail")
    @GetMapping("/posts/{id}")
    public ApiResponse<BlogPostResponse> post(
            @PathVariable Long id,
            @RequestParam(required = false) String language,
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        addLanguageVary(response);
        var locale = languageResolver.resolve(language, acceptLanguage);
        return ApiResponse.success(blogService.publicPost(id, locale));
    }

    private void addLanguageVary(HttpServletResponse response) {
        response.addHeader(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE);
    }
}
