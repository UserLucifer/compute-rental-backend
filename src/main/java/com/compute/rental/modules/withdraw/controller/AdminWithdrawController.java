package com.compute.rental.modules.withdraw.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.modules.withdraw.dto.AdminApproveWithdrawRequest;
import com.compute.rental.modules.withdraw.dto.AdminPaidWithdrawRequest;
import com.compute.rental.modules.withdraw.dto.AdminRejectWithdrawRequest;
import com.compute.rental.modules.withdraw.dto.WithdrawOrderQueryRequest;
import com.compute.rental.modules.withdraw.dto.WithdrawOrderResponse;
import com.compute.rental.modules.system.service.AdminLogService;
import com.compute.rental.modules.withdraw.service.WithdrawService;
import com.compute.rental.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
@Tag(name = "Admin Withdraw")
@RestController
@RequestMapping("/api/admin/withdraw")
public class AdminWithdrawController {

    private final WithdrawService withdrawService;
    private final AdminLogService adminLogService;

    public AdminWithdrawController(WithdrawService withdrawService, AdminLogService adminLogService) {
        this.withdrawService = withdrawService;
        this.adminLogService = adminLogService;
    }

    @Operation(summary = "Admin withdraw orders")
    @GetMapping("/orders")
    public ApiResponse<PageResult<WithdrawOrderResponse>> orders(
            @Valid @ModelAttribute WithdrawOrderQueryRequest request
    ) {
        return ApiResponse.success(withdrawService.pageAdminOrders(request));
    }

    @Operation(summary = "Admin withdraw order detail")
    @GetMapping("/orders/{withdrawNo}")
    public ApiResponse<WithdrawOrderResponse> detail(@PathVariable String withdrawNo) {
        return ApiResponse.success(withdrawService.getAdminOrder(withdrawNo));
    }

    @Operation(summary = "Approve withdraw order")
    @PostMapping("/orders/{withdrawNo}/approve")
    public ApiResponse<WithdrawOrderResponse> approve(
            @PathVariable String withdrawNo,
            @Valid @RequestBody AdminApproveWithdrawRequest request,
            HttpServletRequest httpRequest
    ) {
        var admin = CurrentUser.requiredAdmin();
        var response = withdrawService.approve(withdrawNo, admin.id(), request);
        adminLogService.log(admin.id(), "APPROVE_WITHDRAW", "withdraw_order", null,
                null, withdrawNo, request.reviewRemark(), adminLogService.clientIp(httpRequest));
        return ApiResponse.success(response);
    }

    @Operation(summary = "Reject withdraw order")
    @PostMapping("/orders/{withdrawNo}/reject")
    public ApiResponse<WithdrawOrderResponse> reject(
            @PathVariable String withdrawNo,
            @Valid @RequestBody AdminRejectWithdrawRequest request,
            HttpServletRequest httpRequest
    ) {
        var admin = CurrentUser.requiredAdmin();
        var response = withdrawService.reject(withdrawNo, admin.id(), request);
        adminLogService.log(admin.id(), "REJECT_WITHDRAW", "withdraw_order", null,
                null, withdrawNo, request.reviewRemark(), adminLogService.clientIp(httpRequest));
        return ApiResponse.success(response);
    }

    @Operation(summary = "Mark withdraw order paid")
    @PostMapping("/orders/{withdrawNo}/paid")
    public ApiResponse<WithdrawOrderResponse> paid(
            @PathVariable String withdrawNo,
            @Valid @RequestBody AdminPaidWithdrawRequest request,
            HttpServletRequest httpRequest
    ) {
        var admin = CurrentUser.requiredAdmin();
        var response = withdrawService.paid(withdrawNo, request);
        adminLogService.log(admin.id(), "PAID_WITHDRAW", "withdraw_order", null,
                null, withdrawNo, request.payProofNo(), adminLogService.clientIp(httpRequest));
        return ApiResponse.success(response);
    }
}
