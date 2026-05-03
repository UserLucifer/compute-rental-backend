package com.compute.rental.modules.recharge.dto;

import jakarta.validation.constraints.Size;

public record AdminRejectRechargeRequest(
        @Size(max = 255)
        String reviewRemark
) {
}
