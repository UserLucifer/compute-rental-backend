package com.compute.rental.common.enums;

public enum EmailVerifyStatus {
    UNUSED(0),
    USED(1),
    EXPIRED(2);

    private final int value;

    EmailVerifyStatus(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
