package com.compute.rental.security.jwt;

import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.security.IdentityType;
import com.compute.rental.security.JwtPrincipal;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtTokenProvider {

    private final JwtProperties properties;
    private SecretKey key;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(properties.secret()) || properties.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes for HS256");
        }
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long id, String userId, String role) {
        return createAccessToken(id, userId, role, IdentityType.USER);
    }

    public String createAccessToken(Long id, String userId, String role, IdentityType identityType) {
        var now = Instant.now();
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(String.valueOf(id))
                .claim("userId", userId)
                .claim("role", role)
                .claim("identityType", identityType.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenTtl())))
                .signWith(key)
                .compact();
    }

    public JwtPrincipal parse(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new JwtPrincipal(
                    Long.valueOf(claims.getSubject()),
                    claims.get("userId", String.class),
                    claims.get("role", String.class),
                    identityType(claims.get("identityType", String.class)).name()
            );
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.LOGIN_TOKEN_INVALID);
        }
    }

    private IdentityType identityType(String value) {
        if (!StringUtils.hasText(value)) {
            return IdentityType.USER;
        }
        return IdentityType.valueOf(value);
    }
}
