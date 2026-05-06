package com.compute.rental.modules.recharge.dto;

import jakarta.validation.constraints.NotBlank;

public record RechargeChannelTranslationRequest(
        @NotBlank
        String locale,
        String channelName,
        String accountName
) {
}
