package com.compute.rental.modules.product.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.modules.product.dto.RentalCycleRuleResponse;
import com.compute.rental.modules.product.service.ProductCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Product Catalog")
@RestController
@RequestMapping("/api/rental-cycle-rules")
public class RentalCycleRuleController {

    private final ProductCatalogService productCatalogService;

    public RentalCycleRuleController(ProductCatalogService productCatalogService) {
        this.productCatalogService = productCatalogService;
    }

    @Operation(summary = "Enabled rental cycle rules")
    @GetMapping
    public ApiResponse<List<RentalCycleRuleResponse>> rentalCycleRules() {
        return ApiResponse.success(productCatalogService.listEnabledCycleRules());
    }
}
