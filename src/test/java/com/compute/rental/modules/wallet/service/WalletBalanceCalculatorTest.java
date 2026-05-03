package com.compute.rental.modules.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.modules.wallet.entity.UserWallet;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class WalletBalanceCalculatorTest {

    @Test
    void creditShouldIncreaseAvailableBalance() {
        var change = WalletBalanceCalculator.calculate(wallet("100.00000000", "10.00000000"),
                amount("25.00000000"), WalletChangeAction.IN);

        assertThat(change.afterAvailableBalance()).isEqualByComparingTo("125.00000000");
        assertThat(change.afterFrozenBalance()).isEqualByComparingTo("10.00000000");
    }

    @Test
    void debitShouldDecreaseAvailableBalance() {
        var change = WalletBalanceCalculator.calculate(wallet("100.00000000", "10.00000000"),
                amount("25.00000000"), WalletChangeAction.OUT);

        assertThat(change.afterAvailableBalance()).isEqualByComparingTo("75.00000000");
        assertThat(change.afterFrozenBalance()).isEqualByComparingTo("10.00000000");
    }

    @Test
    void debitShouldRejectInsufficientAvailableBalance() {
        assertThatThrownBy(() -> WalletBalanceCalculator.calculate(wallet("10.00000000", "0.00000000"),
                amount("25.00000000"), WalletChangeAction.OUT))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
    }

    @Test
    void freezeShouldMoveAvailableToFrozen() {
        var change = WalletBalanceCalculator.calculate(wallet("100.00000000", "10.00000000"),
                amount("25.00000000"), WalletChangeAction.FREEZE);

        assertThat(change.afterAvailableBalance()).isEqualByComparingTo("75.00000000");
        assertThat(change.afterFrozenBalance()).isEqualByComparingTo("35.00000000");
    }

    @Test
    void unfreezeShouldMoveFrozenToAvailable() {
        var change = WalletBalanceCalculator.calculate(wallet("100.00000000", "25.00000000"),
                amount("10.00000000"), WalletChangeAction.UNFREEZE);

        assertThat(change.afterAvailableBalance()).isEqualByComparingTo("110.00000000");
        assertThat(change.afterFrozenBalance()).isEqualByComparingTo("15.00000000");
    }

    private UserWallet wallet(String availableBalance, String frozenBalance) {
        var wallet = new UserWallet();
        wallet.setAvailableBalance(amount(availableBalance));
        wallet.setFrozenBalance(amount(frozenBalance));
        return wallet;
    }

    private BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
