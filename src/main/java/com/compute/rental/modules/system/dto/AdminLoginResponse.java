package com.compute.rental.modules.system.dto;

public record AdminLoginResponse(
        String adminAccessToken,
        AdminMeResponse admin
) {
}
