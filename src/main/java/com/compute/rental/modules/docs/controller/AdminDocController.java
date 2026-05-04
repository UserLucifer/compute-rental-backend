package com.compute.rental.modules.docs.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.modules.docs.dto.DocArticleRequest;
import com.compute.rental.modules.docs.dto.DocArticleResponse;
import com.compute.rental.modules.docs.dto.DocCategoryRequest;
import com.compute.rental.modules.docs.dto.DocCategoryResponse;
import com.compute.rental.modules.docs.service.DocService;
import com.compute.rental.modules.system.service.AdminLogService;
import com.compute.rental.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Docs")
@RestController
@RequestMapping("/api/admin/docs")
public class AdminDocController {

    private final DocService docService;
    private final AdminLogService adminLogService;

    public AdminDocController(DocService docService, AdminLogService adminLogService) {
        this.docService = docService;
        this.adminLogService = adminLogService;
    }

    @Operation(summary = "Admin doc categories")
    @GetMapping("/categories")
    public ApiResponse<PageResult<DocCategoryResponse>> categories(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String section,
            @RequestParam(required = false, name = "parent_id") Long parentId,
            @RequestParam(required = false) Integer status
    ) {
        return ApiResponse.success(docService.adminCategories(pageNo, pageSize, language, section, parentId, status));
    }

    @Operation(summary = "Create doc category")
    @PostMapping("/categories")
    public ApiResponse<DocCategoryResponse> createCategory(
            @Valid @RequestBody DocCategoryRequest request,
            HttpServletRequest httpRequest
    ) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(docService.createCategory(request, admin.id(), adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Update doc category")
    @PutMapping("/categories/{id}")
    public ApiResponse<DocCategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody DocCategoryRequest request,
            HttpServletRequest httpRequest
    ) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(docService.updateCategory(id, request, admin.id(), adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Enable doc category")
    @PostMapping("/categories/{id}/enable")
    public ApiResponse<DocCategoryResponse> enableCategory(@PathVariable Long id, HttpServletRequest httpRequest) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(docService.setCategoryStatus(id, CommonStatus.ENABLED.value(), admin.id(),
                adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Disable doc category")
    @PostMapping("/categories/{id}/disable")
    public ApiResponse<DocCategoryResponse> disableCategory(@PathVariable Long id, HttpServletRequest httpRequest) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(docService.setCategoryStatus(id, CommonStatus.DISABLED.value(), admin.id(),
                adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Delete doc category")
    @PostMapping("/categories/{id}/delete")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id, HttpServletRequest httpRequest) {
        var admin = CurrentUser.requiredAdmin();
        docService.deleteCategory(id, admin.id(), adminLogService.clientIp(httpRequest));
        return ApiResponse.success(null);
    }

    @Operation(summary = "Admin doc articles")
    @GetMapping("/articles")
    public ApiResponse<PageResult<DocArticleResponse>> articles(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String section,
            @RequestParam(required = false, name = "category_id") Long categoryId,
            @RequestParam(required = false, name = "publish_status") Integer publishStatus,
            @RequestParam(required = false, name = "is_section_home") Integer isSectionHome,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, name = "start_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false, name = "end_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        return ApiResponse.success(docService.adminArticles(pageNo, pageSize, language, section, categoryId, publishStatus,
                isSectionHome, keyword, startTime, endTime));
    }

    @Operation(summary = "Admin doc article detail")
    @GetMapping("/articles/{id}")
    public ApiResponse<DocArticleResponse> article(@PathVariable Long id) {
        return ApiResponse.success(docService.adminArticle(id));
    }

    @Operation(summary = "Create doc article")
    @PostMapping("/articles")
    public ApiResponse<DocArticleResponse> createArticle(
            @Valid @RequestBody DocArticleRequest request,
            HttpServletRequest httpRequest
    ) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(docService.createArticle(request, admin.id(), adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Update doc article")
    @PutMapping("/articles/{id}")
    public ApiResponse<DocArticleResponse> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody DocArticleRequest request,
            HttpServletRequest httpRequest
    ) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(docService.updateArticle(id, request, admin.id(), adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Publish doc article")
    @PostMapping("/articles/{id}/publish")
    public ApiResponse<DocArticleResponse> publishArticle(@PathVariable Long id, HttpServletRequest httpRequest) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(docService.publishArticle(id, admin.id(), adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Unpublish doc article")
    @PostMapping("/articles/{id}/unpublish")
    public ApiResponse<DocArticleResponse> unpublishArticle(@PathVariable Long id, HttpServletRequest httpRequest) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(docService.unpublishArticle(id, admin.id(), adminLogService.clientIp(httpRequest)));
    }

    @Operation(summary = "Delete doc article")
    @PostMapping("/articles/{id}/delete")
    public ApiResponse<Void> deleteArticle(@PathVariable Long id, HttpServletRequest httpRequest) {
        var admin = CurrentUser.requiredAdmin();
        docService.deleteArticle(id, admin.id(), adminLogService.clientIp(httpRequest));
        return ApiResponse.success(null);
    }
}
