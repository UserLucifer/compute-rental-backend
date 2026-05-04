package com.compute.rental.common.enums;

public enum DocLanguage {
    ZH_CN("zh-CN"),
    EN_US("en-US");

    public static final String DEFAULT_VALUE = "zh-CN";

    private final String value;

    DocLanguage(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
