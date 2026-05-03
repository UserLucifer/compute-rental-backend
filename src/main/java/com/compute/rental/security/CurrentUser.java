package com.compute.rental.security;

import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static JwtPrincipal required() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        }
        return principal;
    }

    public static JwtPrincipal requiredAdmin() {
        var principal = required();
        if (!principal.isAdmin()) {
            throw new BusinessException(ErrorCode.ADMIN_TOKEN_REQUIRED);
        }
        return principal;
    }
}
