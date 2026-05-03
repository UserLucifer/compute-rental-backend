package com.compute.rental.modules.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ApiCredentialResponse(
        String credentialNo,
        String apiName,
        String apiBaseUrl,
        String tokenMasked,
        String modelNameSnapshot,
        BigDecimal deployFeeSnapshot,
        String tokenStatus,
        LocalDateTime generatedAt,
        LocalDateTime activationPaidAt,
        LocalDateTime activatedAt,
        LocalDateTime autoPauseAt,
        LocalDateTime pausedAt,
        LocalDateTime startedAt,
        LocalDateTime expiredAt
) {
}
