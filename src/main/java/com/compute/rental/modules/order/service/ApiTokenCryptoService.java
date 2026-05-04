package com.compute.rental.modules.order.service;

import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ApiTokenCryptoService {

    private static final int IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";

    private final ApiTokenProperties apiTokenProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiTokenCryptoService(ApiTokenProperties apiTokenProperties) {
        this.apiTokenProperties = apiTokenProperties;
    }

    public String encrypt(String plaintext) {
        if (!StringUtils.hasText(plaintext)) {
            throw new BusinessException(ErrorCode.API_TOKEN_PLAINTEXT_REQUIRED);
        }
        try {
            var iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            var cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            var ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(ciphertext);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.API_TOKEN_ENCRYPT_FAILED);
        }
    }

    private SecretKeySpec secretKey() throws Exception {
        if (!StringUtils.hasText(apiTokenProperties.encryptionSecret())) {
            throw new BusinessException(ErrorCode.API_TOKEN_SECRET_NOT_CONFIGURED);
        }
        var digest = MessageDigest.getInstance("SHA-256");
        var key = digest.digest(apiTokenProperties.encryptionSecret().getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, KEY_ALGORITHM);
    }
}
