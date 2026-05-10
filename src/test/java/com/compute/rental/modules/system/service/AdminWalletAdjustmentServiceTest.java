package com.compute.rental.modules.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.enums.WalletBusinessType;
import com.compute.rental.common.enums.WalletTransactionType;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.modules.system.dto.AdminWalletAdjustRequest;
import com.compute.rental.modules.user.entity.AppUser;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import com.compute.rental.modules.wallet.entity.WalletTransaction;
import com.compute.rental.modules.wallet.service.WalletService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminWalletAdjustmentServiceTest {

    @Mock
    private AppUserMapper appUserMapper;
    @Mock
    private WalletService walletService;

    private AdminWalletAdjustmentService service;

    @BeforeEach
    void setUp() {
        service = new AdminWalletAdjustmentService(appUserMapper, walletService);
    }

    @Test
    void adjustInCreditsWalletWithExplicitIdempotencyKey() {
        var request = new AdminWalletAdjustRequest(WalletTransactionType.IN,
                new BigDecimal("12.345678901"), " ADJ001 ", " manual top up ");
        when(appUserMapper.selectById(10L)).thenReturn(user(10L));
        when(walletService.creditWithIdempotencyKey(
                eq(10L),
                eq(new BigDecimal("12.34567890")),
                eq(WalletBusinessType.ADJUST),
                eq("ADJ001"),
                eq("ADJUST:ADJ001"),
                eq("manual top up"))).thenReturn(transaction(WalletTransactionType.IN, "12.34567890"));

        var response = service.adjust(10L, request);

        assertThat(response.txType()).isEqualTo("IN");
        assertThat(response.amount()).isEqualByComparingTo("12.34567890");
        assertThat(response.adjustNo()).isEqualTo("ADJ001");
    }

    @Test
    void adjustOutDebitsWalletWithExplicitIdempotencyKey() {
        var request = new AdminWalletAdjustRequest(WalletTransactionType.OUT,
                new BigDecimal("8.00000000"), "ADJ002", "correction");
        when(appUserMapper.selectById(10L)).thenReturn(user(10L));
        when(walletService.debitWithIdempotencyKey(
                eq(10L),
                eq(new BigDecimal("8.00000000")),
                eq(WalletBusinessType.ADJUST),
                eq("ADJ002"),
                eq("ADJUST:ADJ002"),
                eq("correction"))).thenReturn(transaction(WalletTransactionType.OUT, "8.00000000"));

        var response = service.adjust(10L, request);

        assertThat(response.txType()).isEqualTo("OUT");
        assertThat(response.bizType()).isEqualTo("ADJUST");
    }

    @Test
    void adjustRejectsUnsupportedTransactionType() {
        var request = new AdminWalletAdjustRequest(WalletTransactionType.FREEZE,
                new BigDecimal("8.00000000"), "ADJ003", "bad direction");
        when(appUserMapper.selectById(10L)).thenReturn(user(10L));

        assertThatThrownBy(() -> service.adjust(10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BAD_REQUEST);

        verifyNoInteractions(walletService);
    }

    @Test
    void adjustRejectsReusedAdjustNoWithDifferentTransaction() {
        var request = new AdminWalletAdjustRequest(WalletTransactionType.IN,
                new BigDecimal("8.00000000"), "ADJ004", "top up");
        var existing = transaction(WalletTransactionType.OUT, "8.00000000");
        when(appUserMapper.selectById(10L)).thenReturn(user(10L));
        when(walletService.creditWithIdempotencyKey(
                eq(10L),
                eq(new BigDecimal("8.00000000")),
                eq(WalletBusinessType.ADJUST),
                eq("ADJ004"),
                eq("ADJUST:ADJ004"),
                eq("top up"))).thenReturn(existing);

        assertThatThrownBy(() -> service.adjust(10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT);
    }

    @Test
    void adjustRequiresExistingUser() {
        var request = new AdminWalletAdjustRequest(WalletTransactionType.IN,
                new BigDecimal("8.00000000"), "ADJ005", "top up");
        when(appUserMapper.selectById(10L)).thenReturn(null);

        assertThatThrownBy(() -> service.adjust(10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    private AppUser user(Long id) {
        var user = new AppUser();
        user.setId(id);
        return user;
    }

    private WalletTransaction transaction(WalletTransactionType txType, String amount) {
        var transaction = new WalletTransaction();
        transaction.setId(100L);
        transaction.setTxNo("WT001");
        transaction.setUserId(10L);
        transaction.setWalletId(20L);
        transaction.setCurrency("USDT");
        transaction.setTxType(txType.name());
        transaction.setAmount(new BigDecimal(amount));
        transaction.setBeforeAvailableBalance(new BigDecimal("100.00000000"));
        transaction.setAfterAvailableBalance(WalletTransactionType.IN == txType
                ? new BigDecimal("108.00000000")
                : new BigDecimal("92.00000000"));
        transaction.setBeforeFrozenBalance(BigDecimal.ZERO);
        transaction.setAfterFrozenBalance(BigDecimal.ZERO);
        transaction.setBizType(WalletBusinessType.ADJUST.name());
        transaction.setBizOrderNo("ADJ00" + (WalletTransactionType.IN == txType ? "1" : "2"));
        transaction.setRemark("manual");
        transaction.setCreatedAt(LocalDateTime.now());
        return transaction;
    }
}
