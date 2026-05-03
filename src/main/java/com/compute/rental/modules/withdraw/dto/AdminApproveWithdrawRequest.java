package com.compute.rental.modules.withdraw.dto;

import jakarta.validation.constraints.Size;

public record AdminApproveWithdrawRequest(
        @Size(max = 255)
        String reviewRemark
) {
}
