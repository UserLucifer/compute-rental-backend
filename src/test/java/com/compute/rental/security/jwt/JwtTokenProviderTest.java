package com.compute.rental.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.compute.rental.security.IdentityType;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    @Test
    void tokenShouldKeepIdentityTypeSeparated() {
        var provider = new JwtTokenProvider(new JwtProperties(
                "test",
                "test-secret-must-be-at-least-32-bytes-long",
                Duration.ofHours(1)
        ));
        provider.init();

        var userToken = provider.createAccessToken(1L, "U001", "USER");
        var adminToken = provider.createAccessToken(2L, "admin", "SUPER_ADMIN", IdentityType.ADMIN);

        assertThat(provider.parse(userToken).identityType()).isEqualTo(IdentityType.USER.name());
        assertThat(provider.parse(adminToken).identityType()).isEqualTo(IdentityType.ADMIN.name());
        assertThat(provider.parse(adminToken).isAdmin()).isTrue();
        assertThat(provider.parse(userToken).isUser()).isTrue();
    }
}
