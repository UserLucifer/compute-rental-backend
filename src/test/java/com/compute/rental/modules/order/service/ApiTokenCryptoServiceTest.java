package com.compute.rental.modules.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class ApiTokenCryptoServiceTest {

    @Test
    void encryptShouldReturnIvAndCiphertext() {
        var service = new ApiTokenCryptoService(
                new ApiTokenProperties("test-api-token-secret-32-bytes-value", "https://api.example.invalid/v1"));

        var encrypted = service.encrypt("sk-test-token");

        assertThat(encrypted).contains(":");
        assertThat(encrypted.split(":")).hasSize(2);
    }

    @Test
    void encryptShouldKeepSecretNotConfiguredError() {
        var service = new ApiTokenCryptoService(
                new ApiTokenProperties("", "https://api.example.invalid/v1"));

        assertThatThrownBy(() -> service.encrypt("sk-test-token"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.API_TOKEN_SECRET_NOT_CONFIGURED);
    }
}
