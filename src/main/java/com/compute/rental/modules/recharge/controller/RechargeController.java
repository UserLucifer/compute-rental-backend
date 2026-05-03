package com.compute.rental.modules.recharge.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.modules.recharge.dto.CreateRechargeOrderRequest;
import com.compute.rental.modules.recharge.dto.RechargeChannelResponse;
import com.compute.rental.modules.recharge.dto.RechargeOrderQueryRequest;
import com.compute.rental.modules.recharge.dto.RechargeOrderResponse;
import com.compute.rental.modules.recharge.service.RechargeService;
import com.compute.rental.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "Recharge")
@RestController
@RequestMapping("/api/recharge")
public class RechargeController {

    private final RechargeService rechargeService;

    public RechargeController(RechargeService rechargeService) {
        this.rechargeService = rechargeService;
    }

    @Operation(summary = "Enabled recharge channels")
    @GetMapping("/channels")
    public ApiResponse<List<RechargeChannelResponse>> channels() {
        return ApiResponse.success(rechargeService.listEnabledChannels());
    }

    @Operation(summary = "Submit recharge order")
    @PostMapping("/orders")
    public ApiResponse<RechargeOrderResponse> createOrder(@Valid @RequestBody CreateRechargeOrderRequest request) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(rechargeService.createOrder(currentUser.id(), request));
    }

    @Operation(summary = "Current user recharge orders")
    @GetMapping("/orders")
    public ApiResponse<PageResult<RechargeOrderResponse>> orders(
            @Valid @ModelAttribute RechargeOrderQueryRequest request
    ) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(rechargeService.pageUserOrders(currentUser.id(), request));
    }

    @Operation(summary = "Current user recharge order detail")
    @GetMapping("/orders/{rechargeNo}")
    public ApiResponse<RechargeOrderResponse> detail(@PathVariable String rechargeNo) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(rechargeService.getUserOrder(currentUser.id(), rechargeNo));
    }

    @Operation(summary = "Cancel submitted recharge order")
    @PostMapping("/orders/{rechargeNo}/cancel")
    public ApiResponse<Void> cancel(@PathVariable String rechargeNo) {
        var currentUser = CurrentUser.required();
        rechargeService.cancelUserOrder(currentUser.id(), rechargeNo);
        return ApiResponse.success();
    }
}
