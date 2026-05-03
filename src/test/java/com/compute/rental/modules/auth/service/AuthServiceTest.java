package com.compute.rental.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.EmailVerifyScene;
import com.compute.rental.common.enums.EmailVerifyStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.util.RedisKeys;
import com.compute.rental.modules.auth.dto.SendEmailCodeRequest;
import com.compute.rental.modules.auth.dto.SignupRequest;
import com.compute.rental.modules.auth.dto.VerifyEmailCodeRequest;
import com.compute.rental.modules.auth.support.AuthProperties;
import com.compute.rental.modules.auth.support.VerificationCodeGenerator;
import com.compute.rental.modules.auth.support.VerificationCodeHasher;
import com.compute.rental.modules.user.entity.AppUser;
import com.compute.rental.modules.user.entity.EmailVerifyCode;
import com.compute.rental.modules.user.entity.UserReferralRelation;
import com.compute.rental.modules.user.entity.UserTeamRelation;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import com.compute.rental.modules.user.mapper.EmailVerifyCodeMapper;
import com.compute.rental.modules.user.mapper.UserReferralRelationMapper;
import com.compute.rental.modules.user.mapper.UserTeamRelationMapper;
import com.compute.rental.modules.wallet.entity.UserWallet;
import com.compute.rental.modules.wallet.mapper.UserWalletMapper;
import com.compute.rental.security.jwt.JwtTokenProvider;
import java.time.Duration;
import java.time.LocalDateTime;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "test@example.com";
    private static final String SCENE = "SIGNUP";

    @Mock
    private VerificationCodeGenerator codeGenerator;

    @Mock
    private VerificationCodeHasher codeHasher;

    @Mock
    private EmailService emailService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private EmailVerifyCodeMapper emailVerifyCodeMapper;

    @Mock
    private AppUserMapper appUserMapper;

    @Mock
    private UserWalletMapper userWalletMapper;

    @Mock
    private UserReferralRelationMapper userReferralRelationMapper;

    @Mock
    private UserTeamRelationMapper userTeamRelationMapper;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Captor
    private ArgumentCaptor<EmailVerifyCode> verifyCodeCaptor;

    private AuthService authService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        var configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), AppUser.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), EmailVerifyCode.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserWallet.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserReferralRelation.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserTeamRelation.class);
    }

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                new AuthProperties(Duration.ofMinutes(5), 6, 5, null, null),
                codeGenerator,
                codeHasher,
                emailService,
                redisTemplate,
                emailVerifyCodeMapper,
                appUserMapper,
                userWalletMapper,
                userReferralRelationMapper,
                userTeamRelationMapper,
                jwtTokenProvider,
                passwordEncoder
        );
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(codeHasher.normalizeEmail(" Test@Example.com ")).thenReturn(EMAIL);
    }

    @Test
    void sendSignupEmailCodeShouldSetCooldownRateLimitAndPersistCode() {
        when(appUserMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(valueOperations.setIfAbsent(eq(RedisKeys.emailCodeCooldown(EMAIL, SCENE)), eq("1"), any(Duration.class)))
                .thenReturn(true);
        when(valueOperations.increment(RedisKeys.emailCodeRate(EMAIL, SCENE))).thenReturn(1L);
        when(codeGenerator.generate(6)).thenReturn("123456");
        when(codeHasher.hash(EMAIL, SCENE, "123456")).thenReturn("hash");

        authService.sendSignupEmailCode(new SendEmailCodeRequest(" Test@Example.com "), "127.0.0.1");

        verify(redisTemplate).expire(RedisKeys.emailCodeRate(EMAIL, SCENE), Duration.ofSeconds(60));
        verify(emailVerifyCodeMapper).insert(verifyCodeCaptor.capture());
        var saved = verifyCodeCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getScene()).isEqualTo(SCENE);
        assertThat(saved.getCodeHash()).isEqualTo("hash");
        assertThat(saved.getStatus()).isEqualTo(EmailVerifyStatus.UNUSED.value());
        verify(emailService).sendSignupCode(EMAIL, "123456");
    }

    @Test
    void sendSignupEmailCodeShouldRejectWhenCooldownExists() {
        when(appUserMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(valueOperations.setIfAbsent(eq(RedisKeys.emailCodeCooldown(EMAIL, SCENE)), eq("1"), any(Duration.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.sendSignupEmailCode(
                new SendEmailCodeRequest(" Test@Example.com "), "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_CODE_SEND_TOO_FREQUENTLY);

        verify(emailVerifyCodeMapper, never()).insert(any(EmailVerifyCode.class));
        verify(emailService, never()).sendSignupCode(any(), any());
    }

    @Test
    void sendSignupEmailCodeShouldFailClosedWhenRedisUnavailable() {
        when(appUserMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(valueOperations.setIfAbsent(eq(RedisKeys.emailCodeCooldown(EMAIL, SCENE)), eq("1"), any(Duration.class)))
                .thenThrow(new DataAccessResourceFailureException("redis down"));

        assertThatThrownBy(() -> authService.sendSignupEmailCode(
                new SendEmailCodeRequest(" Test@Example.com "), "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_CODE_RATE_LIMIT_UNAVAILABLE);

        verify(emailVerifyCodeMapper, never()).insert(any(EmailVerifyCode.class));
        verify(emailService, never()).sendSignupCode(any(), any());
    }

    @Test
    void verifySignupEmailCodeShouldRecordFailedAttemptWhenCodeIsWrong() {
        when(appUserMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(valueOperations.get(RedisKeys.emailCodeAttempts(EMAIL, SCENE))).thenReturn(null);
        when(emailVerifyCodeMapper.selectOne(any(Wrapper.class))).thenReturn(code("hash"));
        when(codeHasher.hash(EMAIL, SCENE, "000000")).thenReturn("wrong-hash");
        when(valueOperations.increment(RedisKeys.emailCodeAttempts(EMAIL, SCENE))).thenReturn(1L);

        assertThatThrownBy(() -> authService.verifySignupEmailCode(
                new VerifyEmailCodeRequest(" Test@Example.com ", "000000")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_CODE_INVALID_OR_EXPIRED);

        verify(redisTemplate).expire(RedisKeys.emailCodeAttempts(EMAIL, SCENE), Duration.ofMinutes(5));
    }

    @Test
    void verifySignupEmailCodeShouldRejectAfterMaxAttemptsWithoutReadingDatabase() {
        when(appUserMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(valueOperations.get(RedisKeys.emailCodeAttempts(EMAIL, SCENE))).thenReturn("5");

        assertThatThrownBy(() -> authService.verifySignupEmailCode(
                new VerifyEmailCodeRequest(" Test@Example.com ", "123456")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_CODE_ATTEMPTS_EXCEEDED);

        verify(emailVerifyCodeMapper, never()).selectOne(any(Wrapper.class));
    }

    @Test
    void verifySignupEmailCodeShouldFailClosedWhenAttemptRedisUnavailable() {
        when(appUserMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(valueOperations.get(RedisKeys.emailCodeAttempts(EMAIL, SCENE)))
                .thenThrow(new DataAccessResourceFailureException("redis down"));

        assertThatThrownBy(() -> authService.verifySignupEmailCode(
                new VerifyEmailCodeRequest(" Test@Example.com ", "123456")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_CODE_ATTEMPT_LIMIT_UNAVAILABLE);

        verify(emailVerifyCodeMapper, never()).selectOne(any(Wrapper.class));
    }

    @Test
    void signupShouldConsumeCodeAndClearAttemptKey() {
        when(appUserMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(valueOperations.get(RedisKeys.emailCodeAttempts(EMAIL, SCENE))).thenReturn(null);
        when(emailVerifyCodeMapper.selectOne(any(Wrapper.class))).thenReturn(code("hash"));
        when(codeHasher.hash(EMAIL, SCENE, "123456")).thenReturn("hash");
        when(codeHasher.normalizeEmail(" Test@Example.com ")).thenReturn(EMAIL);
        when(passwordEncoder.encode("password123")).thenReturn("pwd-hash");
        when(jwtTokenProvider.createAccessToken(any(), any(), eq("USER"))).thenReturn("jwt-token");
        when(appUserMapper.insert(any(AppUser.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, AppUser.class).setId(10L);
            return 1;
        });

        var response = authService.signup(new SignupRequest(
                " Test@Example.com ",
                "123456",
                "tester",
                "password123",
                null
        ));

        verify(redisTemplate).delete(RedisKeys.emailCodeAttempts(EMAIL, SCENE));
        verify(emailVerifyCodeMapper).updateById(verifyCodeCaptor.capture());
        assertThat(verifyCodeCaptor.getValue().getStatus()).isEqualTo(EmailVerifyStatus.USED.value());
        assertThat(response.accessToken()).isEqualTo("jwt-token");
    }

    @Test
    void signupShouldRejectAlreadyConsumedCodeAndRecordAttempt() {
        when(appUserMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(valueOperations.get(RedisKeys.emailCodeAttempts(EMAIL, SCENE))).thenReturn(null);
        when(emailVerifyCodeMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(valueOperations.increment(RedisKeys.emailCodeAttempts(EMAIL, SCENE))).thenReturn(1L);

        assertThatThrownBy(() -> authService.signup(new SignupRequest(
                " Test@Example.com ",
                "123456",
                "tester",
                "password123",
                null
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_CODE_INVALID_OR_EXPIRED);

        verify(appUserMapper, never()).insert(any(AppUser.class));
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
}
