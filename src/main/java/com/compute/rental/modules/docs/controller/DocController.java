package com.compute.rental.modules.docs.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.modules.docs.dto.DocArticleResponse;
import com.compute.rental.modules.docs.dto.DocCategoryResponse;
import com.compute.rental.modules.docs.service.DocService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Docs")
@RestController
@RequestMapping("/api/docs")
public class DocController {

    private final DocService docService;

    public DocController(DocService docService) {
        this.docService = docService;
    }

    @Operation(summary = "Public doc category tree")
    @GetMapping("/categories")
    public ApiResponse<List<DocCategoryResponse>> categories() {
        return ApiResponse.success(docService.publicCategories());
    }

    @Operation(summary = "Public doc articles")
    @GetMapping("/articles")
    public ApiResponse<PageResult<DocArticleResponse>> articles(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false, name = "category_id") Long categoryId,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(docService.publicArticles(pageNo, pageSize, categoryId, keyword));
    }

    @Operation(summary = "Public doc article detail")
    @GetMapping("/articles/{id}")
    public ApiResponse<DocArticleResponse> article(@PathVariable Long id) {
        return ApiResponse.success(docService.publicArticle(id));
    }

    @Operation(summary = "Public doc article detail by slug")
    @GetMapping("/articles/slug/{slug}")
    public ApiResponse<DocArticleResponse> articleBySlug(@PathVariable String slug) {
        return ApiResponse.success(docService.publicArticleBySlug(slug));
    }

    @Operation(summary = "Public doc article search")
    @GetMapping("/search")
    public ApiResponse<PageResult<DocArticleResponse>> search(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(docService.publicSearch(pageNo, pageSize, keyword));
    }
}
