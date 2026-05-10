package com.compute.rental.security.jwt;

import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.modules.system.mapper.SysAdminMapper;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import com.compute.rental.security.JwtPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final AppUserMapper appUserMapper;
    private final SysAdminMapper sysAdminMapper;

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            AppUserMapper appUserMapper,
            SysAdminMapper sysAdminMapper
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.appUserMapper = appUserMapper;
        this.sysAdminMapper = sysAdminMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var token = resolveToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                var principal = jwtTokenProvider.parse(token);
                if (isEnabled(principal)) {
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.identityType()));
                    var authentication = new UsernamePasswordAuthenticationToken(principal, token, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (RuntimeException ex) {
                log.debug("JWT authentication failed", ex);
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        var authorization = request.getHeader(AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length());
    }

    private boolean isEnabled(JwtPrincipal principal) {
        if (principal.isAdmin()) {
            var admin = sysAdminMapper.selectById(principal.id());
            return admin != null && Integer.valueOf(CommonStatus.ENABLED.value()).equals(admin.getStatus());
        }
        if (principal.isUser()) {
            var user = appUserMapper.selectById(principal.id());
            return user != null && Integer.valueOf(CommonStatus.ENABLED.value()).equals(user.getStatus());
        }
        return false;
    }
}
