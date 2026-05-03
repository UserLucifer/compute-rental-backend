package com.compute.rental.common.enums;

public enum BlogPublishStatus {
    DRAFT(0),
    PUBLISHED(1),
    OFFLINE(2);

    private final int value;

    BlogPublishStatus(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
