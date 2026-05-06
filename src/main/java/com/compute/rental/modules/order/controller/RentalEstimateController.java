package com.compute.rental.modules.order.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.i18n.LanguageResolver;
import com.compute.rental.modules.order.dto.RentalEstimateRequest;
import com.compute.rental.modules.order.dto.RentalEstimateResponse;
import com.compute.rental.modules.order.service.RentalEstimateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Rental")
@RestController
@RequestMapping("/api/rental")
public class RentalEstimateController {

    private final RentalEstimateService rentalEstimateService;
    private final LanguageResolver languageResolver;

    public RentalEstimateController(RentalEstimateService rentalEstimateService, LanguageResolver languageResolver) {
        this.rentalEstimateService = rentalEstimateService;
        this.languageResolver = languageResolver;
    }

    @Operation(summary = "Estimate rental profit")
    @PostMapping("/estimate")
    public ApiResponse<RentalEstimateResponse> estimate(
            @Valid @RequestBody RentalEstimateRequest request,
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            HttpServletResponse response
    ) {
        response.addHeader(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE);
        var locale = languageResolver.resolve(request.language(), acceptLanguage);
        return ApiResponse.success(rentalEstimateService.estimate(request, locale));
    }
}
