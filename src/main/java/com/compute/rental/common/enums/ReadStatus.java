package com.compute.rental.common.enums;

public enum ReadStatus {
    UNREAD(0),
    READ(1);

    private final int value;

    ReadStatus(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
