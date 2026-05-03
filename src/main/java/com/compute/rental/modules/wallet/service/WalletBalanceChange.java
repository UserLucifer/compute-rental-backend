package com.compute.rental.modules.wallet.service;

import java.math.BigDecimal;

record WalletBalanceChange(
        BigDecimal beforeAvailableBalance,
        BigDecimal afterAvailableBalance,
        BigDecimal beforeFrozenBalance,
        BigDecimal afterFrozenBalance
) {
}
