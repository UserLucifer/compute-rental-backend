package com.compute.rental.modules.withdraw.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.modules.withdraw.dto.CreateWithdrawOrderRequest;
import com.compute.rental.modules.withdraw.dto.WithdrawOrderQueryRequest;
import com.compute.rental.modules.withdraw.dto.WithdrawOrderResponse;
import com.compute.rental.modules.withdraw.service.WithdrawService;
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
@Tag(name = "Withdraw")
@RestController
@RequestMapping("/api/withdraw")
public class WithdrawController {

    private final WithdrawService withdrawService;

    public WithdrawController(WithdrawService withdrawService) {
        this.withdrawService = withdrawService;
    }

    @Operation(summary = "Submit withdraw order")
    @PostMapping("/orders")
    public ApiResponse<WithdrawOrderResponse> createOrder(@Valid @RequestBody CreateWithdrawOrderRequest request) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(withdrawService.createOrder(currentUser.id(), request));
    }

    @Operation(summary = "Current user withdraw orders")
    @GetMapping("/orders")
    public ApiResponse<PageResult<WithdrawOrderResponse>> orders(
            @Valid @ModelAttribute WithdrawOrderQueryRequest request
    ) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(withdrawService.pageUserOrders(currentUser.id(), request));
    }

    @Operation(summary = "Current user withdraw order detail")
    @GetMapping("/orders/{withdrawNo}")
    public ApiResponse<WithdrawOrderResponse> detail(@PathVariable String withdrawNo) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(withdrawService.getUserOrder(currentUser.id(), withdrawNo));
    }

    @Operation(summary = "Cancel pending withdraw order")
    @PostMapping("/orders/{withdrawNo}/cancel")
    public ApiResponse<Void> cancel(@PathVariable String withdrawNo) {
        var currentUser = CurrentUser.required();
        withdrawService.cancelUserOrder(currentUser.id(), withdrawNo);
        return ApiResponse.success();
    }
}
