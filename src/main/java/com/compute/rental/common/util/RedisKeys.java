package com.compute.rental.common.util;

import java.util.List;

public final class RedisKeys {

    public static final String CACHE_PREFIX = "compute-rental:cache:";
    public static final String LOCK_PREFIX = "compute-rental:lock:";
    public static final String SYS_CONFIG_CACHE_PREFIX = CACHE_PREFIX + "sys-config:";
    public static final String CATALOG_CACHE_PREFIX = CACHE_PREFIX + "catalog:";
    public static final String EMAIL_CODE_PREFIX = "compute-rental:email-code:";
    public static final String EMAIL_CODE_RATE_PREFIX = EMAIL_CODE_PREFIX + "rate:";
    public static final String EMAIL_CODE_COOLDOWN_PREFIX = EMAIL_CODE_PREFIX + "cooldown:";
    public static final String EMAIL_CODE_ATTEMPTS_PREFIX = EMAIL_CODE_PREFIX + "attempts:";

    public static final List<String> ADMIN_CLEARABLE_PREFIXES = List.of(
            CACHE_PREFIX,
            EMAIL_CODE_PREFIX
    );

    private RedisKeys() {
    }

    public static String sysConfig(String configKey) {
        return SYS_CONFIG_CACHE_PREFIX + configKey;
    }

    public static String catalogRegions() {
        return CATALOG_CACHE_PREFIX + "regions";
    }

    public static String catalogGpuModels(Long regionId) {
        return CATALOG_CACHE_PREFIX + "gpu-models:" + (regionId == null ? "all" : "region:" + regionId);
    }

    public static String catalogProductPage(long current, long size, Long regionId, Long gpuModelId) {
        return CATALOG_CACHE_PREFIX + "products:page:" + current
                + ":size:" + size
                + ":region:" + (regionId == null ? "all" : regionId)
                + ":gpu:" + (gpuModelId == null ? "all" : gpuModelId);
    }

    public static String catalogProduct(String productCode) {
        return CATALOG_CACHE_PREFIX + "product:" + productCode;
    }

    public static String catalogAiModels() {
        return CATALOG_CACHE_PREFIX + "ai-models";
    }

    public static String catalogCycleRules() {
        return CATALOG_CACHE_PREFIX + "cycle-rules";
    }

    public static String orderOperationLock(String orderNo, String operation) {
        return LOCK_PREFIX + "order:" + orderNo + ":" + operation;
    }

    public static String withdrawCreateLock(Long userId) {
        return LOCK_PREFIX + "withdraw:create:" + userId;
    }

    public static String withdrawOperationLock(String withdrawNo, String operation) {
        return LOCK_PREFIX + "withdraw:" + withdrawNo + ":" + operation;
    }

    public static String rechargeCreateLock(Long userId) {
        return LOCK_PREFIX + "recharge:create:" + userId;
    }

    public static String rechargeOperationLock(String rechargeNo, String operation) {
        return LOCK_PREFIX + "recharge:" + rechargeNo + ":" + operation;
    }

    public static String emailCodeRate(String email, String scene) {
        return EMAIL_CODE_RATE_PREFIX + email + ":" + scene;
    }

    public static String emailCodeCooldown(String email, String scene) {
        return EMAIL_CODE_COOLDOWN_PREFIX + email + ":" + scene;
    }

    public static String emailCodeAttempts(String email, String scene) {
        return EMAIL_CODE_ATTEMPTS_PREFIX + email + ":" + scene;
    }
}
