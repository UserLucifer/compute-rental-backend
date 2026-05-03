package com.compute.rental.modules.auth.support;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class VerificationCodeGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate(int length) {
        var bound = (int) Math.pow(10, length);
        var floor = bound / 10;
        return String.valueOf(floor + secureRandom.nextInt(bound - floor));
    }
}
