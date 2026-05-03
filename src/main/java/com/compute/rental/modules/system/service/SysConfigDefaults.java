package com.compute.rental.modules.system.service;

import java.util.Map;

public final class SysConfigDefaults {

    public static final String WITHDRAW_MIN_AMOUNT = "withdraw.min_amount";
    public static final String WITHDRAW_FEE_FREE_THRESHOLD = "withdraw.fee_free_threshold";
    public static final String WITHDRAW_FEE_RATE = "withdraw.fee_rate";
    public static final String WITHDRAW_MAX_DAILY_AMOUNT = "withdraw.max_daily_amount";
    public static final String RECHARGE_MIN_AMOUNT = "recharge.min_amount";
    public static final String ORDER_ACTIVATION_TIMEOUT_MINUTES = "order.activation_timeout_minutes";
    public static final String ORDER_PENDING_ACTIVATION_TIMEOUT_MINUTES = "order.pending_activation_timeout_minutes";
    public static final String EMAIL_CODE_RATE_LIMIT_PER_MINUTE = "email_code.rate_limit_per_minute";

    private static final Map<String, String> DEFAULTS = Map.of(
            WITHDRAW_MIN_AMOUNT, "10",
            WITHDRAW_FEE_FREE_THRESHOLD, "100",
            WITHDRAW_FEE_RATE, "0.05",
            WITHDRAW_MAX_DAILY_AMOUNT, "100000",
            RECHARGE_MIN_AMOUNT, "500",
            ORDER_ACTIVATION_TIMEOUT_MINUTES, "60",
            ORDER_PENDING_ACTIVATION_TIMEOUT_MINUTES, "60",
            EMAIL_CODE_RATE_LIMIT_PER_MINUTE, "5"
    );

    private SysConfigDefaults() {
    }

    public static String defaultValue(String key) {
        return DEFAULTS.get(key);
    }
}
