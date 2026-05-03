package com.compute.rental.modules.product.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.modules.product.dto.AiModelResponse;
import com.compute.rental.modules.product.service.ProductCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Product Catalog")
@RestController
@RequestMapping("/api/ai-models")
public class AiModelController {

    private final ProductCatalogService productCatalogService;

    public AiModelController(ProductCatalogService productCatalogService) {
        this.productCatalogService = productCatalogService;
    }

    @Operation(summary = "Enabled AI models")
    @GetMapping
    public ApiResponse<List<AiModelResponse>> aiModels() {
        return ApiResponse.success(productCatalogService.listEnabledAiModels());
    }
}
