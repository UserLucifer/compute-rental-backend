package com.compute.rental.common.enums;

public enum DocSection {
    GUIDE("guide"),
    INTEGRATION("integration"),
    FAQ("faq"),
    SUPPORT("support");

    private final String value;

    DocSection(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
