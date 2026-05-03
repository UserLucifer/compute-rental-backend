package com.compute.rental.modules.auth.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class VerificationCodeHasher {

    public String hash(String email, String scene, String code) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var normalized = normalizeEmail(email) + ":" + scene + ":" + code;
            return HexFormat.of().formatHex(digest.digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    public String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
