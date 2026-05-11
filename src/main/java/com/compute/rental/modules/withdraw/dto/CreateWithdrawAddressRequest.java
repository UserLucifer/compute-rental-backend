package com.compute.rental.modules.withdraw.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWithdrawAddressRequest(
        @NotBlank
        @Size(max = 64)
        String network,

        @Size(max = 64)
        String accountName,

        @NotBlank
        @Size(max = 255)
        String accountNo,

        @Size(max = 64)
        String label,

        Boolean defaultAddress
) {
}
