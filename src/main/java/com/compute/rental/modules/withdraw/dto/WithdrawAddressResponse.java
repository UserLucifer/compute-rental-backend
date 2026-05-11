package com.compute.rental.modules.withdraw.dto;

import java.time.LocalDateTime;

public record WithdrawAddressResponse(
        Long addressId,
        String network,
        String accountName,
        String accountNo,
        String label,
        Boolean defaultAddress,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
