package com.compute.rental.modules.system.dto;

public record AdminMeResponse(
        Long adminId,
        String userName,
        Integer status,
        String role
) {
}
