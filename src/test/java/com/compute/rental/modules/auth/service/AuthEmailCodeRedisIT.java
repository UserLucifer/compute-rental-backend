package com.compute.rental.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.compute.rental.common.enums.EmailVerifyScene;
import com.compute.rental.common.enums.EmailVerifyStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.util.RedisKeys;
import com.compute.rental.modules.auth.dto.SendEmailCodeRequest;
import com.compute.rental.modules.auth.dto.VerifyEmailCodeRequest;
import com.compute.rental.modules.auth.support.AuthProperties;
import com.compute.rental.modules.auth.support.VerificationCodeGenerator;
import com.compute.rental.modules.auth.support.VerificationCodeHasher;
import com.compute.rental.modules.user.entity.EmailVerifyCode;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import com.compute.rental.modules.user.mapper.EmailVerifyCodeMapper;
import com.compute.rental.modules.user.mapper.UserReferralRelationMapper;
import com.compute.rental.modules.user.mapper.UserTeamRelationMapper;
import com.compute.rental.modules.wallet.mapper.UserWalletMapper;
import com.compute.rental.security.jwt.JwtTokenProvider;
import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

class AuthEmailCodeRedisIT {

    private static final String EMAIL = "it-auth@example.com";
    private static final String SCENE = EmailVerifyScene.SIGNUP.name();

    private StringRedisTemplate redisTemplate;
    private LettuceConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() {
        redisTemplate = realRedisTemplate();
        assertThat(redisTemplate.execute((RedisCallback<String>) connection -> connection.ping())).isEqualTo("PONG");
        cleanupKeys();
    }

    @AfterEach
    void tearDown() {
        if (redisTemplate != null) {
            cleanupKeys();
        }
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void sendShouldCreateCooldownAndRateLimitKeysAgainstRealRedis() {
        var service = authService("123456", "hash", "hash");

        service.sendSignupEmailCode(new SendEmailCodeRequest(EMAIL), "127.0.0.1");

        assertThat(redisTemplate.hasKey(RedisKeys.emailCodeCooldown(EMAIL, SCENE))).isTrue();
        assertThat(redisTemplate.getExpire(RedisKeys.emailCodeCooldown(EMAIL, SCENE))).isPositive();
        assertThat(redisTemplate.opsForValue().get(RedisKeys.emailCodeRate(EMAIL, SCENE))).isEqualTo("1");

        assertThatThrownBy(() -> service.sendSignupEmailCode(new SendEmailCodeRequest(EMAIL), "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_CODE_SEND_TOO_FREQUENTLY);
    }

    @Test
    void wrongCodeShouldLockVerificationAfterMaxAttemptsAgainstRealRedis() {
        var service = authService("123456", "hash", "wrong-hash");

        for (var i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.verifySignupEmailCode(new VerifyEmailCodeRequest(EMAIL, "000000")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EMAIL_CODE_INVALID_OR_EXPIRED);
        }

        assertThatThrownBy(() -> service.verifySignupEmailCode(new VerifyEmailCodeRequest(EMAIL, "000000")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_CODE_ATTEMPTS_EXCEEDED);

        assertThat(redisTemplate.opsForValue().get(RedisKeys.emailCodeAttempts(EMAIL, SCENE))).isEqualTo("5");
        assertThat(redisTemplate.getExpire(RedisKeys.emailCodeAttempts(EMAIL, SCENE))).isPositive();
    }

    private AuthService authService(String generatedCode, String storedHash, String submittedHash) {
        var generator = mock(VerificationCodeGenerator.class);
        var hasher = mock(VerificationCodeHasher.class);
        var emailService = mock(EmailService.class);
        var codeMapper = mock(EmailVerifyCodeMapper.class);
        var appUserMapper = mock(AppUserMapper.class);
        var userWalletMapper = mock(UserWalletMapper.class);
        var referralMapper = mock(UserReferralRelationMapper.class);
        var teamMapper = mock(UserTeamRelationMapper.class);
        var jwtTokenProvider = mock(JwtTokenProvider.class);
        var passwordEncoder = mock(PasswordEncoder.class);

        when(generator.generate(6)).thenReturn(generatedCode);
        when(hasher.normalizeEmail(EMAIL)).thenReturn(EMAIL);
        when(hasher.hash(EMAIL, SCENE, generatedCode)).thenReturn(storedHash);
        when(hasher.hash(EMAIL, SCENE, "000000")).thenReturn(submittedHash);
        when(appUserMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(codeMapper.selectOne(any(Wrapper.class))).thenReturn(code(storedHash));

        return new AuthService(
                new AuthProperties(Duration.ofMinutes(5), 6, 5, null, null),
                generator,
                hasher,
                emailService,
                redisTemplate,
                codeMapper,
                appUserMapper,
                userWalletMapper,
                referralMapper,
                teamMapper,
                jwtTokenProvider,
                passwordEncoder
        );
    }

    private EmailVerifyCode code(String hash) {
        var code = new EmailVerifyCode();
        code.setId(1L);
        code.setEmail(EMAIL);
        code.setScene(SCENE);
        code.setCodeHash(hash);
        code.setStatus(EmailVerifyStatus.UNUSED.value());
        code.setExpireAt(LocalDateTime.now().plusMinutes(5));
        return code;
    }

    private void cleanupKeys() {
        redisTemplate.delete(List.of(
                RedisKeys.emailCodeCooldown(EMAIL, SCENE),
                RedisKeys.emailCodeRate(EMAIL, SCENE),
                RedisKeys.emailCodeAttempts(EMAIL, SCENE)
        ));
    }

    private StringRedisTemplate realRedisTemplate() {
        var properties = redisProperties();
        var host = property(properties, "REDIS_HOST", "spring.data.redis.host", "localhost");
        var port = Integer.parseInt(property(properties, "REDIS_PORT", "spring.data.redis.port", "6379"));
        var database = Integer.parseInt(property(properties, "REDIS_DATABASE", "spring.data.redis.database", "0"));
        var password = property(properties, "REDIS_PASSWORD", "spring.data.redis.password", "");

        var redisConfig = new RedisStandaloneConfiguration(host, port);
        redisConfig.setDatabase(database);
        if (StringUtils.hasText(password)) {
            redisConfig.setPassword(RedisPassword.of(password));
        }
        var clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(3))
                .shutdownTimeout(Duration.ZERO)
                .build();
        connectionFactory = new LettuceConnectionFactory(redisConfig, clientConfig);
        connectionFactory.setValidateConnection(true);
        connectionFactory.afterPropertiesSet();

        var template = new StringRedisTemplate(connectionFactory);
        template.afterPropertiesSet();
        return template;
    }

    private Properties redisProperties() {
        var properties = new Properties();
        properties.putAll(loadYaml(new ClassPathResource("application.yml")));
        var localConfig = new File("application-local.yml");
        if (localConfig.exists()) {
            properties.putAll(loadYaml(new FileSystemResource(localConfig)));
        }
        return properties;
    }

    private Properties loadYaml(org.springframework.core.io.Resource resource) {
        var factory = new YamlPropertiesFactoryBean();
        factory.setResources(resource);
        var properties = factory.getObject();
        return properties == null ? new Properties() : properties;
    }

    private String property(Properties properties, String envName, String propertyName, String defaultValue) {
        var envValue = System.getenv(envName);
        if (StringUtils.hasText(envValue)) {
            return envValue;
        }
        var propertyValue = properties.get(propertyName);
        if (propertyValue != null && StringUtils.hasText(String.valueOf(propertyValue))) {
            return String.valueOf(propertyValue);
        }
        return defaultValue;
    }
}
