package com.compute.rental.modules.wallet.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.modules.wallet.dto.WalletMeResponse;
import com.compute.rental.modules.wallet.dto.WalletTransactionQueryRequest;
import com.compute.rental.modules.wallet.dto.WalletTransactionResponse;
import com.compute.rental.modules.wallet.service.WalletService;
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
@Tag(name = "Wallet")
@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @Operation(summary = "Current user wallet")
    @GetMapping("/me")
    public ApiResponse<WalletMeResponse> me() {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(walletService.getCurrentUserWallet(currentUser.id()));
    }

    @Operation(summary = "Current user wallet transactions")
    @GetMapping("/transactions")
    public ApiResponse<PageResult<WalletTransactionResponse>> transactions(
            @Valid @ModelAttribute WalletTransactionQueryRequest request
    ) {
        var currentUser = CurrentUser.required();
        return ApiResponse.success(walletService.pageCurrentUserTransactions(currentUser.id(), request));
    }
}
