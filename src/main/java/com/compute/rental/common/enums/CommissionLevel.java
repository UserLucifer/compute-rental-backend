package com.compute.rental.common.enums;

public enum CommissionLevel {
    LEVEL_1(1),
    LEVEL_2(2);

    private final int levelNo;

    CommissionLevel(int levelNo) {
        this.levelNo = levelNo;
    }

    public int levelNo() {
        return levelNo;
    }
}
