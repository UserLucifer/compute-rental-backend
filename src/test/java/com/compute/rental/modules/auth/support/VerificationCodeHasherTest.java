package com.compute.rental.modules.auth.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VerificationCodeHasherTest {

    private final VerificationCodeHasher hasher = new VerificationCodeHasher();

    @Test
    void hashShouldBeStableAndNormalizeEmail() {
        var first = hasher.hash("User@Example.com", "LOGIN", "123456");
        var second = hasher.hash("user@example.com", "LOGIN", "123456");

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64);
        assertThat(first).doesNotContain("123456");
    }
}
