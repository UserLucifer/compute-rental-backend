package com.compute.rental.modules.order.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.page.PageParam;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.modules.order.dto.ApiDeployInfoResponse;
import com.compute.rental.modules.order.service.RentalOrderService;
import com.compute.rental.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "API Management")
@RestController
@RequestMapping("/api/rental/api-management")
public class ApiManagementController {

    private final RentalOrderService rentalOrderService;

    public ApiManagementController(RentalOrderService rentalOrderService) {
        this.rentalOrderService = rentalOrderService;
    }

    @Operation(summary = "Current user API management list")
    @GetMapping
    public ApiResponse<PageResult<ApiDeployInfoResponse>> list(@Valid @ModelAttribute PageParam request) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(rentalOrderService.pageUserApiManagement(currentUser.id(), request));
    }
}
