package com.compute.rental.modules.product.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.i18n.LanguageResolver;
import com.compute.rental.modules.product.dto.RentalCycleRuleResponse;
import com.compute.rental.modules.product.service.ProductCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Product Catalog")
@RestController
@RequestMapping("/api/rental-cycle-rules")
public class RentalCycleRuleController {

    private final ProductCatalogService productCatalogService;
    private final LanguageResolver languageResolver;

    public RentalCycleRuleController(ProductCatalogService productCatalogService, LanguageResolver languageResolver) {
        this.productCatalogService = productCatalogService;
        this.languageResolver = languageResolver;
    }

    @Operation(summary = "Enabled rental cycle rules")
    @GetMapping
    public ApiResponse<List<RentalCycleRuleResponse>> rentalCycleRules(
            @RequestParam(required = false) String language,
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        response.addHeader(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE);
        var locale = languageResolver.resolve(language, acceptLanguage);
        return ApiResponse.success(productCatalogService.listEnabledCycleRules(locale));
    }
}
