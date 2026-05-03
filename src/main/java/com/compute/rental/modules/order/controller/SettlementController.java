package com.compute.rental.modules.order.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.modules.order.dto.SettlementOrderQueryRequest;
import com.compute.rental.modules.order.dto.SettlementOrderResponse;
import com.compute.rental.modules.order.service.SettlementService;
import com.compute.rental.security.CurrentUser;
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
@Tag(name = "Settlement")
@RestController
@RequestMapping("/api/settlement/orders")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @Operation(summary = "Current user settlement orders")
    @GetMapping
    public ApiResponse<PageResult<SettlementOrderResponse>> orders(
            @Valid @ModelAttribute SettlementOrderQueryRequest request
    ) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(settlementService.pageUserSettlementOrders(currentUser.id(), request));
    }

    @Operation(summary = "Current user settlement order detail")
    @GetMapping("/{settlementNo}")
    public ApiResponse<SettlementOrderResponse> detail(@PathVariable String settlementNo) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(settlementService.getUserSettlementOrder(currentUser.id(), settlementNo));
    }
}
