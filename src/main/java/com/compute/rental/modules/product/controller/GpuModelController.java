package com.compute.rental.modules.product.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.modules.product.dto.GpuModelResponse;
import com.compute.rental.modules.product.service.ProductCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Product Catalog")
@RestController
@RequestMapping("/api/gpu-models")
public class GpuModelController {

    private final ProductCatalogService productCatalogService;

    public GpuModelController(ProductCatalogService productCatalogService) {
        this.productCatalogService = productCatalogService;
    }

    @Operation(summary = "Enabled GPU models")
    @GetMapping
    public ApiResponse<List<GpuModelResponse>> gpuModels(
            @RequestParam(required = false) Long regionId
    ) {
        return ApiResponse.success(productCatalogService.listEnabledGpuModels(regionId));
    }
}
