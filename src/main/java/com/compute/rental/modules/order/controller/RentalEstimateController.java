package com.compute.rental.modules.order.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.modules.order.dto.RentalEstimateRequest;
import com.compute.rental.modules.order.dto.RentalEstimateResponse;
import com.compute.rental.modules.order.service.RentalEstimateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Rental")
@RestController
@RequestMapping("/api/rental")
public class RentalEstimateController {

    private final RentalEstimateService rentalEstimateService;

    public RentalEstimateController(RentalEstimateService rentalEstimateService) {
        this.rentalEstimateService = rentalEstimateService;
    }

    @Operation(summary = "Estimate rental profit")
    @PostMapping("/estimate")
    public ApiResponse<RentalEstimateResponse> estimate(@Valid @RequestBody RentalEstimateRequest request) {
        return ApiResponse.success(rentalEstimateService.estimate(request));
    }
}
