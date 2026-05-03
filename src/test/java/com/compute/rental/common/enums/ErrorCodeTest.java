package com.compute.rental.common.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class ErrorCodeTest {

    @Test
    void errorCodesShouldBeUnique() {
        var codes = new HashSet<Integer>();

        for (var errorCode : ErrorCode.values()) {
            assertThat(codes.add(errorCode.code()))
                    .as("duplicate error code: " + errorCode.code() + " on " + errorCode.name())
                    .isTrue();
        }
    }

    @Test
    void everyErrorCodeShouldHaveMessageAndHttpStatus() {
        for (var errorCode : ErrorCode.values()) {
            assertThat(errorCode.message()).as(errorCode.name()).isNotBlank();
            assertThat(errorCode.httpStatus()).as(errorCode.name()).isNotNull();
        }
    }
}
