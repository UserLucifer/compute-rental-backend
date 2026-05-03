package com.compute.rental.modules.wallet.service;

import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.modules.wallet.entity.UserWallet;
import java.math.BigDecimal;

final class WalletBalanceCalculator {

    private WalletBalanceCalculator() {
    }

    static WalletBalanceChange calculate(UserWallet wallet, BigDecimal amount, WalletChangeAction action) {
        var beforeAvailable = wallet.getAvailableBalance();
        var beforeFrozen = wallet.getFrozenBalance();
        return switch (action) {
            case IN -> new WalletBalanceChange(
                    beforeAvailable,
                    beforeAvailable.add(amount),
                    beforeFrozen,
                    beforeFrozen
            );
            case OUT -> {
                requireEnough(beforeAvailable, amount);
                yield new WalletBalanceChange(
                        beforeAvailable,
                        beforeAvailable.subtract(amount),
                        beforeFrozen,
                        beforeFrozen
                );
            }
            case FREEZE -> {
                requireEnough(beforeAvailable, amount);
                yield new WalletBalanceChange(
                        beforeAvailable,
                        beforeAvailable.subtract(amount),
                        beforeFrozen,
                        beforeFrozen.add(amount)
                );
            }
            case UNFREEZE -> {
                requireEnough(beforeFrozen, amount);
                yield new WalletBalanceChange(
                        beforeAvailable,
                        beforeAvailable.add(amount),
                        beforeFrozen,
                        beforeFrozen.subtract(amount)
                );
            }
            case OUT_FROM_FROZEN -> {
                requireEnough(beforeFrozen, amount);
                yield new WalletBalanceChange(
                        beforeAvailable,
                        beforeAvailable,
                        beforeFrozen,
                        beforeFrozen.subtract(amount)
                );
            }
        };
    }

    private static void requireEnough(BigDecimal balance, BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
    }
}
