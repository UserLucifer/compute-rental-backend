package com.compute.rental.common.util;

import java.util.UUID;

public final class IdempotencyKeyGenerator {

    private IdempotencyKeyGenerator() {
    }

    public static String newKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
