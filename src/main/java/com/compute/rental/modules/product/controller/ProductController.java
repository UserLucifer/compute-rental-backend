package com.compute.rental.modules.product.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.modules.product.dto.ProductQueryRequest;
import com.compute.rental.modules.product.dto.ProductResponse;
import com.compute.rental.modules.product.service.ProductCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "Product Catalog")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductCatalogService productCatalogService;

    public ProductController(ProductCatalogService productCatalogService) {
        this.productCatalogService = productCatalogService;
    }

    @Operation(summary = "Enabled products")
    @GetMapping
    public ApiResponse<PageResult<ProductResponse>> products(@Valid @ModelAttribute ProductQueryRequest request) {
        return ApiResponse.success(productCatalogService.pageEnabledProducts(request));
    }

    @Operation(summary = "Enabled product detail")
    @GetMapping("/{productCode}")
    public ApiResponse<ProductResponse> detail(@PathVariable String productCode) {
        return ApiResponse.success(productCatalogService.getEnabledProduct(productCode));
    }
}
