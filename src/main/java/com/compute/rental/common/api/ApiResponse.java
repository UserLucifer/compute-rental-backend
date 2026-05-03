package com.compute.rental.common.api;

import com.compute.rental.common.enums.ErrorCode;
import java.time.LocalDateTime;

public record ApiResponse<T>(
        int code,
        String message,
        T data,
        LocalDateTime timestamp
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.code(), ErrorCode.SUCCESS.message(), data, LocalDateTime.now());
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.code(), errorCode.message(), null, LocalDateTime.now());
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.code(), message, null, LocalDateTime.now());
    }
}
