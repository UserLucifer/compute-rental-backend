package com.compute.rental.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.compute.rental.common.enums.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.bind.MissingServletRequestParameterException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void businessExceptionShouldUseBusinessErrorCodeAndHttpStatus() {
        var response = handler.handleBusinessException(new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED));

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED.httpStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED.code());
        assertThat(response.getBody().message()).isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED.message());
    }

    @Test
    void malformedJsonShouldUseRequestBodyInvalidCode() {
        var response = handler.handleHttpMessageNotReadable(
                new HttpMessageNotReadableException("invalid json", new MockHttpInputMessage(new byte[0])));

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.REQUEST_BODY_INVALID.httpStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.REQUEST_BODY_INVALID.code());
    }

    @Test
    void missingParameterShouldUseStableParameterCodeAndKeepParameterName() {
        var response = handler.handleMissingServletRequestParameter(
                new MissingServletRequestParameterException("pageNo", "Long"));

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.REQUEST_PARAMETER_MISSING.httpStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.REQUEST_PARAMETER_MISSING.code());
        assertThat(response.getBody().message()).contains("pageNo");
    }
}
