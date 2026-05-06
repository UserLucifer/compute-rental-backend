package com.compute.rental.modules.product.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.i18n.LanguageResolver;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.modules.product.dto.ProductQueryRequest;
import com.compute.rental.modules.product.dto.ProductResponse;
import com.compute.rental.modules.product.service.ProductCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "Product Catalog")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductCatalogService productCatalogService;
    private final LanguageResolver languageResolver;

    public ProductController(ProductCatalogService productCatalogService, LanguageResolver languageResolver) {
        this.productCatalogService = productCatalogService;
        this.languageResolver = languageResolver;
    }

    @Operation(summary = "Enabled products")
    @GetMapping
    public ApiResponse<PageResult<ProductResponse>> products(
            @Valid @ModelAttribute ProductQueryRequest request,
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        addLanguageVary(response);
        var locale = languageResolver.resolve(request.language(), acceptLanguage);
        return ApiResponse.success(productCatalogService.pageEnabledProducts(request, locale));
    }

    @Operation(summary = "Enabled product detail")
    @GetMapping("/{productCode}")
    public ApiResponse<ProductResponse> detail(
            @PathVariable String productCode,
            @RequestParam(required = false) String language,
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        addLanguageVary(response);
        var locale = languageResolver.resolve(language, acceptLanguage);
        return ApiResponse.success(productCatalogService.getEnabledProduct(productCode, locale));
    }

    private void addLanguageVary(HttpServletResponse response) {
        response.addHeader(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE);
    }
}
