package com.compute.rental.modules.order.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.modules.order.dto.ApiCredentialResponse;
import com.compute.rental.modules.order.dto.ApiDeployInfoResponse;
import com.compute.rental.modules.order.dto.ApiDeployOrderResponse;
import com.compute.rental.modules.order.dto.CreateRentalOrderRequest;
import com.compute.rental.modules.order.dto.ProfitRecordQueryRequest;
import com.compute.rental.modules.order.dto.ProfitRecordResponse;
import com.compute.rental.modules.order.dto.RentalOrderDetailResponse;
import com.compute.rental.modules.order.dto.RentalOrderQueryRequest;
import com.compute.rental.modules.order.dto.RentalOrderSummaryResponse;
import com.compute.rental.modules.order.dto.SettlementOrderResponse;
import com.compute.rental.modules.order.service.ProfitService;
import com.compute.rental.modules.order.service.RentalOrderService;
import com.compute.rental.modules.order.service.SettlementService;
import com.compute.rental.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "Rental")
@RestController
@RequestMapping("/api/rental/orders")
public class RentalOrderController {

    private final RentalOrderService rentalOrderService;
    private final ProfitService profitService;
    private final SettlementService settlementService;

    public RentalOrderController(
            RentalOrderService rentalOrderService,
            ProfitService profitService,
            SettlementService settlementService
    ) {
        this.rentalOrderService = rentalOrderService;
        this.profitService = profitService;
        this.settlementService = settlementService;
    }

    @Operation(summary = "Create rental order")
    @PostMapping
    public ApiResponse<RentalOrderDetailResponse> create(@Valid @RequestBody CreateRentalOrderRequest request) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(rentalOrderService.createOrder(currentUser.id(), request));
    }

    @Operation(summary = "Current user rental orders")
    @GetMapping
    public ApiResponse<PageResult<RentalOrderSummaryResponse>> orders(
            @Valid @ModelAttribute RentalOrderQueryRequest request
    ) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(rentalOrderService.pageUserOrders(currentUser.id(), request));
    }

    @Operation(summary = "Current user rental order detail")
    @GetMapping("/{orderNo}")
    public ApiResponse<RentalOrderDetailResponse> detail(@PathVariable String orderNo) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(rentalOrderService.getUserOrder(currentUser.id(), orderNo));
    }

    @Operation(summary = "Pay rental machine fee")
    @PostMapping("/{orderNo}/pay")
    public ApiResponse<RentalOrderDetailResponse> pay(@PathVariable String orderNo) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(rentalOrderService.payMachineFee(currentUser.id(), orderNo));
    }

    @Operation(summary = "Cancel rental order")
    @PostMapping("/{orderNo}/cancel")
    public ApiResponse<RentalOrderDetailResponse> cancel(@PathVariable String orderNo) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(rentalOrderService.cancelOrder(currentUser.id(), orderNo));
    }

    @Operation(summary = "Current user rental order API credential")
    @GetMapping("/{orderNo}/api-credential")
    public ApiResponse<ApiCredentialResponse> apiCredential(@PathVariable String orderNo) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(rentalOrderService.getUserApiCredential(currentUser.id(), orderNo));
    }

    @Operation(summary = "Current user rental order profit records")
    @GetMapping("/{orderNo}/profits")
    public ApiResponse<PageResult<ProfitRecordResponse>> profits(
            @PathVariable String orderNo,
            @Valid @ModelAttribute ProfitRecordQueryRequest request
    ) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(profitService.pageOrderProfitRecords(currentUser.id(), orderNo, request));
    }

    @Operation(summary = "Settle current user rental order early")
    @PostMapping("/{orderNo}/settle-early")
    public ApiResponse<SettlementOrderResponse> settleEarly(@PathVariable String orderNo) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(settlementService.settleEarly(currentUser.id(), orderNo));
    }

    @Operation(summary = "Current user API deploy info")
    @GetMapping("/{orderNo}/deploy-info")
    public ApiResponse<ApiDeployInfoResponse> deployInfo(@PathVariable String orderNo) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(rentalOrderService.getDeployInfo(currentUser.id(), orderNo));
    }

    @Operation(summary = "Pay API deploy fee")
    @PostMapping("/{orderNo}/deploy/pay")
    public ApiResponse<ApiDeployOrderResponse> payDeployFee(@PathVariable String orderNo) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(rentalOrderService.payDeployFee(currentUser.id(), orderNo));
    }

    @Operation(summary = "Current user API deploy order")
    @GetMapping("/{orderNo}/deploy-order")
    public ApiResponse<ApiDeployOrderResponse> deployOrder(@PathVariable String orderNo) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(rentalOrderService.getDeployOrder(currentUser.id(), orderNo));
    }

    @Operation(summary = "Start paused rental order")
    @PostMapping("/{orderNo}/start")
    public ApiResponse<RentalOrderDetailResponse> start(@PathVariable String orderNo) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(rentalOrderService.startOrder(currentUser.id(), orderNo));
    }
}
