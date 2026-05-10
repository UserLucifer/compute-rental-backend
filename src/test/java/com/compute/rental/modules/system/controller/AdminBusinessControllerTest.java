package com.compute.rental.modules.system.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.compute.rental.common.enums.WalletTransactionType;
import com.compute.rental.modules.system.dto.AdminWalletAdjustRequest;
import com.compute.rental.modules.system.dto.AdminWalletAdjustResponse;
import com.compute.rental.modules.system.service.AdminBusinessQueryService;
import com.compute.rental.modules.system.service.AdminLogService;
import com.compute.rental.modules.system.service.AdminWalletAdjustmentService;
import com.compute.rental.security.IdentityType;
import com.compute.rental.security.JwtPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AdminBusinessControllerTest {

    @Mock
    private AdminBusinessQueryService adminBusinessQueryService;
    @Mock
    private AdminWalletAdjustmentService adminWalletAdjustmentService;
    @Mock
    private AdminLogService adminLogService;
    @Mock
    private HttpServletRequest httpServletRequest;

    private AdminBusinessController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminBusinessController(adminBusinessQueryService, adminWalletAdjustmentService,
                adminLogService);
        var principal = new JwtPrincipal(1L, "admin", "SUPER_ADMIN", IdentityType.ADMIN.name());
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(principal, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adjustWalletWritesAdminLog() {
        var request = new AdminWalletAdjustRequest(WalletTransactionType.IN,
                new BigDecimal("10.00000000"), "ADJ001", "manual top up");
        var response = new AdminWalletAdjustResponse(
                100L,
                "WT001",
                10L,
                20L,
                "USDT",
                "IN",
                new BigDecimal("10.00000000"),
                new BigDecimal("100.00000000"),
                new BigDecimal("110.00000000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "ADJUST",
                "ADJ001",
                "manual top up",
                LocalDateTime.now()
        );
        when(adminWalletAdjustmentService.adjust(10L, request)).thenReturn(response);
        when(adminLogService.clientIp(httpServletRequest)).thenReturn("127.0.0.1");

        controller.adjustWallet(10L, request, httpServletRequest);

        verify(adminLogService).log(eq(1L), eq(AdminLogService.ADMIN_WALLET_ADJUST), eq("wallet_transaction"),
                eq(100L), isNull(), eq("WT001"),
                eq("userId=10,txType=IN,amount=10.00000000,adjustNo=ADJ001,reason=manual top up"),
                eq("127.0.0.1"));
    }
}
