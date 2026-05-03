package com.compute.rental.common.exception;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.enums.ErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        var errorCode = ex.errorCode();
        return ResponseEntity.status(errorCode.httpStatus()).body(ApiResponse.fail(errorCode, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        var errors = validationErrors(ex.getBindingResult().getFieldErrors());
        var body = new ApiResponse<>(
                ErrorCode.VALIDATION_FAILED.code(),
                ErrorCode.VALIDATION_FAILED.message(),
                errors,
                java.time.LocalDateTime.now()
        );
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.httpStatus()).body(body);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleBindException(BindException ex) {
        var body = new ApiResponse<>(
                ErrorCode.VALIDATION_FAILED.code(),
                ErrorCode.VALIDATION_FAILED.message(),
                validationErrors(ex.getBindingResult().getFieldErrors()),
                java.time.LocalDateTime.now()
        );
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.httpStatus()).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        var message = new StringJoiner("；");
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            message.add(localizeConstraintViolation(violation));
        }
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.httpStatus())
                .body(ApiResponse.fail(ErrorCode.VALIDATION_FAILED, message.toString()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(ErrorCode.REQUEST_BODY_INVALID.httpStatus())
                .body(ApiResponse.fail(ErrorCode.REQUEST_BODY_INVALID));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex) {
        return ResponseEntity.status(ErrorCode.REQUEST_PARAMETER_MISSING.httpStatus())
                .body(ApiResponse.fail(ErrorCode.REQUEST_PARAMETER_MISSING, "缺少必填参数：" + ex.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(ErrorCode.REQUEST_PARAMETER_TYPE_MISMATCH.httpStatus())
                .body(ApiResponse.fail(ErrorCode.REQUEST_PARAMETER_TYPE_MISMATCH, "请求参数格式错误：" + ex.getName()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(ErrorCode.SYSTEM_ERROR.httpStatus()).body(ApiResponse.fail(ErrorCode.SYSTEM_ERROR));
    }

    private Map<String, String> validationErrors(Iterable<FieldError> fieldErrors) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : fieldErrors) {
            errors.put(fieldError.getField(), localizeFieldError(fieldError));
        }
        return errors;
    }

    private String localizeFieldError(FieldError fieldError) {
        var code = fieldError.getCode();
        if (code == null) {
            return ErrorCode.VALIDATION_FAILED.message();
        }
        return switch (code) {
            case "NotBlank", "NotNull", "NotEmpty" -> "不能为空";
            case "Email" -> "邮箱格式不正确";
            case "Pattern" -> "格式不正确";
            case "Size" -> "长度不符合要求";
            case "Min", "DecimalMin" -> "不能小于最小值";
            case "Max", "DecimalMax" -> "不能大于最大值";
            case "Positive" -> "必须大于 0";
            case "PositiveOrZero" -> "不能小于 0";
            default -> ErrorCode.VALIDATION_FAILED.message();
        };
    }

    private String localizeConstraintViolation(ConstraintViolation<?> violation) {
        var field = violation.getPropertyPath() == null ? "参数" : violation.getPropertyPath().toString();
        var annotation = violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
        var message = switch (annotation) {
            case "NotBlank", "NotNull", "NotEmpty" -> "不能为空";
            case "Email" -> "邮箱格式不正确";
            case "Pattern" -> "格式不正确";
            case "Size" -> "长度不符合要求";
            case "Min", "DecimalMin" -> "不能小于最小值";
            case "Max", "DecimalMax" -> "不能大于最大值";
            case "Positive" -> "必须大于 0";
            case "PositiveOrZero" -> "不能小于 0";
            default -> ErrorCode.VALIDATION_FAILED.message();
        };
        return field + "：" + message;
    }
}
