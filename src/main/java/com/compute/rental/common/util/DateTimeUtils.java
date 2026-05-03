package com.compute.rental.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class DateTimeUtils {

    public static final ZoneId PLATFORM_ZONE = ZoneId.of("Asia/Shanghai");

    private DateTimeUtils() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(PLATFORM_ZONE);
    }

    public static LocalDate today() {
        return LocalDate.now(PLATFORM_ZONE);
    }
}
