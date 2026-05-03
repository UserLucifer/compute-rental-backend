package com.compute.rental.modules.withdraw.dto;

import jakarta.validation.constraints.Size;

public record AdminPaidWithdrawRequest(
        @Size(max = 128)
        String payProofNo
) {
}
