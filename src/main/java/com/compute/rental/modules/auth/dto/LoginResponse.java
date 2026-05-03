package com.compute.rental.modules.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        UserProfile user
) {

    public record UserProfile(
            Long id,
            String userId,
            String email,
            String userName,
            String avatarKey
    ) {
    }
}
