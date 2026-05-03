package com.compute.rental.security;

public record JwtPrincipal(Long id, String userId, String role, String identityType) {

    public boolean isAdmin() {
        return IdentityType.ADMIN.name().equals(identityType);
    }

    public boolean isUser() {
        return IdentityType.USER.name().equals(identityType);
    }
}
