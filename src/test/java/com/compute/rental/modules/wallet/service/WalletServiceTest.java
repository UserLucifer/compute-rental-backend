package com.compute.rental.modules.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.enums.WalletBusinessType;
import com.compute.rental.common.enums.WalletTransactionType;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import com.compute.rental.modules.wallet.entity.UserWallet;
import com.compute.rental.modules.wallet.entity.WalletTransaction;
import com.compute.rental.modules.wallet.mapper.UserWalletMapper;
import com.compute.rental.modules.wallet.mapper.WalletTransactionMapper;
import java.math.BigDecimal;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private UserWalletMapper userWalletMapper;

    @Mock
    private WalletTransactionMapper walletTransactionMapper;

    @Mock
    private AppUserMapper appUserMapper;

    @Captor
    private ArgumentCaptor<WalletTransaction> transactionCaptor;

    @Captor
    private ArgumentCaptor<Wrapper<UserWallet>> walletUpdateCaptor;

    @InjectMocks
    private WalletService walletService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        var configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserWallet.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), WalletTransaction.class);
    }

    @Test
    void creditShouldUpdateWalletAndInsertTransaction() {
        when(walletTransactionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(userWalletMapper.selectOne(any(Wrapper.class))).thenReturn(wallet());
        when(userWalletMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(walletTransactionMapper.insert(any(WalletTransaction.class))).thenReturn(1);

        var tx = walletService.credit(1L, new BigDecimal("20.00000000"),
                WalletBusinessType.RECHARGE, "R001", "test");

        verify(walletTransactionMapper).insert(transactionCaptor.capture());
        var inserted = transactionCaptor.getValue();
        assertThat(tx).isSameAs(inserted);
        assertThat(inserted.getIdempotencyKey()).isEqualTo("RECHARGE:R001:IN");
        assertThat(inserted.getTxType()).isEqualTo("IN");
        assertThat(inserted.getAmount()).isEqualByComparingTo("20.00000000");
        assertThat(inserted.getBeforeAvailableBalance()).isEqualByComparingTo("100.00000000");
        assertThat(inserted.getAfterAvailableBalance()).isEqualByComparingTo("120.00000000");
    }

    @Test
    void rechargeCreditShouldUpdateTotalRecharge() {
        when(walletTransactionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(userWalletMapper.selectOne(any(Wrapper.class))).thenReturn(wallet());
        when(userWalletMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(walletTransactionMapper.insert(any(WalletTransaction.class))).thenReturn(1);

        walletService.credit(1L, new BigDecimal("20.00000000"),
                WalletBusinessType.RECHARGE, "R001", "test");

        verify(userWalletMapper).update(isNull(), walletUpdateCaptor.capture());
        assertThat(sqlSet(walletUpdateCaptor.getValue())).contains("total_recharge");
    }

    @Test
    void rentProfitCreditShouldUpdateTotalProfit() {
        when(walletTransactionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(userWalletMapper.selectOne(any(Wrapper.class))).thenReturn(wallet());
        when(userWalletMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(walletTransactionMapper.insert(any(WalletTransaction.class))).thenReturn(1);

        walletService.creditWithIdempotencyKey(1L, new BigDecimal("8.00000000"),
                WalletBusinessType.RENT_PROFIT, "P001", "RENT_PROFIT:RO001:2026-05-03", "profit");

        verify(userWalletMapper).update(isNull(), walletUpdateCaptor.capture());
        assertThat(sqlSet(walletUpdateCaptor.getValue())).contains("total_profit");
    }

    @Test
    void commissionCreditShouldUpdateTotalCommission() {
        when(walletTransactionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(userWalletMapper.selectOne(any(Wrapper.class))).thenReturn(wallet());
        when(userWalletMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(walletTransactionMapper.insert(any(WalletTransaction.class))).thenReturn(1);

        walletService.creditWithIdempotencyKey(1L, new BigDecimal("3.00000000"),
                WalletBusinessType.COMMISSION_PROFIT, "C001", "COMMISSION_PROFIT:1:1", "commission");

        verify(userWalletMapper).update(isNull(), walletUpdateCaptor.capture());
        assertThat(sqlSet(walletUpdateCaptor.getValue())).contains("total_commission");
    }

    @Test
    void duplicatedIdempotencyKeyShouldReturnExistingTransactionWithoutChangingWallet() {
        var existing = new WalletTransaction();
        existing.setIdempotencyKey("RECHARGE:R001:IN");
        when(walletTransactionMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        var result = walletService.credit(1L, new BigDecimal("20.00000000"),
                WalletBusinessType.RECHARGE, "R001", "test");

        assertThat(result).isSameAs(existing);
        verify(userWalletMapper, never()).update(any(), any());
        verify(walletTransactionMapper, never()).insert(any(WalletTransaction.class));
    }

    @Test
    void debitShouldDecreaseAvailableBalanceAndUseActionIdempotencyKey() {
        when(walletTransactionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(userWalletMapper.selectOne(any(Wrapper.class))).thenReturn(wallet());
        when(userWalletMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(walletTransactionMapper.insert(any(WalletTransaction.class))).thenReturn(1);

        walletService.debit(1L, new BigDecimal("30.00000000"),
                WalletBusinessType.RENT_PAY, "RO001", "PAY", "machine fee");

        verify(walletTransactionMapper).insert(transactionCaptor.capture());
        var inserted = transactionCaptor.getValue();
        assertThat(inserted.getIdempotencyKey()).isEqualTo("RENT_PAY:RO001:PAY");
        assertThat(inserted.getTxType()).isEqualTo(WalletTransactionType.OUT.name());
        assertThat(inserted.getBeforeAvailableBalance()).isEqualByComparingTo("100.00000000");
        assertThat(inserted.getAfterAvailableBalance()).isEqualByComparingTo("70.00000000");
    }

    @Test
    void debitWithIdempotencyKeyShouldUseExplicitKey() {
        when(walletTransactionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(userWalletMapper.selectOne(any(Wrapper.class))).thenReturn(wallet());
        when(userWalletMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(walletTransactionMapper.insert(any(WalletTransaction.class))).thenReturn(1);

        walletService.debitWithIdempotencyKey(1L, new BigDecimal("30.00000000"),
                WalletBusinessType.ADJUST, "ADJ001", "ADJUST:ADJ001", "manual correction");

        verify(walletTransactionMapper).insert(transactionCaptor.capture());
        var inserted = transactionCaptor.getValue();
        assertThat(inserted.getIdempotencyKey()).isEqualTo("ADJUST:ADJ001");
        assertThat(inserted.getBizType()).isEqualTo(WalletBusinessType.ADJUST.name());
        assertThat(inserted.getBizOrderNo()).isEqualTo("ADJ001");
        assertThat(inserted.getTxType()).isEqualTo(WalletTransactionType.OUT.name());
    }

    @Test
    void freezeShouldMoveAvailableToFrozen() {
        when(walletTransactionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(userWalletMapper.selectOne(any(Wrapper.class))).thenReturn(wallet());
        when(userWalletMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(walletTransactionMapper.insert(any(WalletTransaction.class))).thenReturn(1);

        walletService.freeze(1L, new BigDecimal("40.00000000"),
                WalletBusinessType.WITHDRAW, "WD001", "FREEZE", "withdraw freeze");

        verify(walletTransactionMapper).insert(transactionCaptor.capture());
        var inserted = transactionCaptor.getValue();
        assertThat(inserted.getIdempotencyKey()).isEqualTo("WITHDRAW:WD001:FREEZE");
        assertThat(inserted.getTxType()).isEqualTo(WalletTransactionType.FREEZE.name());
        assertThat(inserted.getAfterAvailableBalance()).isEqualByComparingTo("60.00000000");
        assertThat(inserted.getAfterFrozenBalance()).isEqualByComparingTo("40.00000000");
    }

    @Test
    void unfreezeShouldMoveFrozenToAvailable() {
        when(walletTransactionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(userWalletMapper.selectOne(any(Wrapper.class))).thenReturn(walletWithBalances("60.00000000", "40.00000000"));
        when(userWalletMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(walletTransactionMapper.insert(any(WalletTransaction.class))).thenReturn(1);

        walletService.unfreeze(1L, new BigDecimal("15.00000000"),
                WalletBusinessType.WITHDRAW, "WD001", "UNFREEZE", "withdraw canceled");

        verify(walletTransactionMapper).insert(transactionCaptor.capture());
        var inserted = transactionCaptor.getValue();
        assertThat(inserted.getIdempotencyKey()).isEqualTo("WITHDRAW:WD001:UNFREEZE");
        assertThat(inserted.getTxType()).isEqualTo(WalletTransactionType.UNFREEZE.name());
        assertThat(inserted.getAfterAvailableBalance()).isEqualByComparingTo("75.00000000");
        assertThat(inserted.getAfterFrozenBalance()).isEqualByComparingTo("25.00000000");
    }

    @Test
    void deductFrozenShouldDecreaseFrozenAndUpdateTotalWithdraw() {
        when(walletTransactionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(userWalletMapper.selectOne(any(Wrapper.class))).thenReturn(walletWithBalances("60.00000000", "40.00000000"));
        when(userWalletMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(walletTransactionMapper.insert(any(WalletTransaction.class))).thenReturn(1);

        walletService.deductFrozen(1L, new BigDecimal("40.00000000"), new BigDecimal("37.50000000"),
                WalletBusinessType.WITHDRAW, "WD001", "PAID", "withdraw paid");

        verify(userWalletMapper).update(isNull(), walletUpdateCaptor.capture());
        verify(walletTransactionMapper).insert(transactionCaptor.capture());
        var inserted = transactionCaptor.getValue();
        assertThat(sqlSet(walletUpdateCaptor.getValue())).contains("total_withdraw");
        assertThat(inserted.getIdempotencyKey()).isEqualTo("WITHDRAW:WD001:PAID");
        assertThat(inserted.getAfterAvailableBalance()).isEqualByComparingTo("60.00000000");
        assertThat(inserted.getAfterFrozenBalance()).isEqualByComparingTo("0E-8");
    }

    @Test
    void recordNoBalanceChangeShouldInsertTransactionWithoutWalletUpdate() {
        when(walletTransactionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(userWalletMapper.selectOne(any(Wrapper.class))).thenReturn(wallet());
        when(walletTransactionMapper.insert(any(WalletTransaction.class))).thenReturn(1);

        walletService.recordNoBalanceChange(1L, new BigDecimal("12.00000000"),
                WalletTransactionType.OUT, WalletBusinessType.EARLY_PENALTY, "SO001",
                "EARLY_PENALTY:RO001:EARLY_TERMINATE", "penalty");

        verify(userWalletMapper, never()).update(any(), any());
        verify(walletTransactionMapper).insert(transactionCaptor.capture());
        var inserted = transactionCaptor.getValue();
        assertThat(inserted.getIdempotencyKey()).isEqualTo("EARLY_PENALTY:RO001:EARLY_TERMINATE");
        assertThat(inserted.getBeforeAvailableBalance()).isEqualByComparingTo("100.00000000");
        assertThat(inserted.getAfterAvailableBalance()).isEqualByComparingTo("100.00000000");
    }

    @Test
    void insufficientAvailableBalanceShouldNotUpdateWalletOrInsertTransaction() {
        when(walletTransactionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(userWalletMapper.selectOne(any(Wrapper.class))).thenReturn(wallet());

        assertThatThrownBy(() -> walletService.debit(1L, new BigDecimal("120.00000000"),
                WalletBusinessType.RENT_PAY, "RO001", "PAY", "machine fee"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);

        verify(userWalletMapper, never()).update(any(), any());
        verify(walletTransactionMapper, never()).insert(any(WalletTransaction.class));
    }

    @Test
    void optimisticLockFailureShouldNotInsertTransaction() {
        when(walletTransactionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(userWalletMapper.selectOne(any(Wrapper.class))).thenReturn(wallet());
        when(userWalletMapper.update(isNull(), any(Wrapper.class))).thenReturn(0);
        when(userWalletMapper.selectById(10L)).thenReturn(walletWithBalances("100.00000000", "0.00000000"));

        assertThatThrownBy(() -> walletService.debit(1L, new BigDecimal("30.00000000"),
                WalletBusinessType.RENT_PAY, "RO001", "PAY", "machine fee"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONCURRENT_UPDATE_FAILED);

        verify(walletTransactionMapper, never()).insert(any(WalletTransaction.class));
    }

    @Test
    void optimisticLockFailureShouldReportLatestInsufficientBalance() {
        when(walletTransactionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(userWalletMapper.selectOne(any(Wrapper.class))).thenReturn(wallet());
        when(userWalletMapper.update(isNull(), any(Wrapper.class))).thenReturn(0);
        when(userWalletMapper.selectById(10L)).thenReturn(walletWithBalances("20.00000000", "0.00000000"));

        assertThatThrownBy(() -> walletService.debit(1L, new BigDecimal("30.00000000"),
                WalletBusinessType.RENT_PAY, "RO001", "PAY", "machine fee"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);

        verify(walletTransactionMapper, never()).insert(any(WalletTransaction.class));
    }

    @Test
    void duplicateInsertShouldRaiseIdempotencyConflictAfterWalletUpdateAttempt() {
        when(walletTransactionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(userWalletMapper.selectOne(any(Wrapper.class))).thenReturn(wallet());
        when(userWalletMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(walletTransactionMapper.insert(any(WalletTransaction.class))).thenThrow(new DuplicateKeyException("duplicate"));

        assertThatThrownBy(() -> walletService.credit(1L, new BigDecimal("20.00000000"),
                WalletBusinessType.RECHARGE, "R001", "test"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WALLET_IDEMPOTENCY_KEY_DUPLICATE);
    }

    private UserWallet wallet() {
        return walletWithBalances("100.00000000", "0.00000000");
    }

    private UserWallet walletWithBalances(String availableBalance, String frozenBalance) {
        var wallet = new UserWallet();
        wallet.setId(10L);
        wallet.setUserId(1L);
        wallet.setWalletNo("W001");
        wallet.setCurrency("USDT");
        wallet.setAvailableBalance(new BigDecimal(availableBalance));
        wallet.setFrozenBalance(new BigDecimal(frozenBalance));
        wallet.setTotalRecharge(BigDecimal.ZERO);
        wallet.setTotalWithdraw(BigDecimal.ZERO);
        wallet.setTotalProfit(BigDecimal.ZERO);
        wallet.setTotalCommission(BigDecimal.ZERO);
        wallet.setStatus(CommonStatus.ENABLED.value());
        wallet.setVersionNo(0);
        return wallet;
    }

    private String sqlSet(Wrapper<UserWallet> wrapper) {
        return ((LambdaUpdateWrapper<UserWallet>) wrapper).getSqlSet();
    }
}
