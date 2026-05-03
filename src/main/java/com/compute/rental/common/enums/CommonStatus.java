package com.compute.rental.common.enums;

public enum CommonStatus {
    ENABLED(1),
    DISABLED(0);

    private final int value;

    CommonStatus(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
