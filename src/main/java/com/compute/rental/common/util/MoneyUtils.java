package com.compute.rental.common.util;

import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {

    public static final int SCALE = 8;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_DOWN);

    private MoneyUtils() {
    }

    public static BigDecimal scale(BigDecimal amount) {
        if (amount == null) {
            return ZERO;
        }
        return amount.setScale(SCALE, RoundingMode.HALF_DOWN);
    }

    public static BigDecimal requireNonNegative(BigDecimal amount) {
        var scaled = scale(amount);
        if (scaled.signum() < 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT, "金额不能为负数");
        }
        return scaled;
    }
}
