package com.compute.rental.modules.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.modules.system.dto.AdminLoginRequest;
import com.compute.rental.modules.system.entity.SysAdmin;
import com.compute.rental.modules.system.mapper.SysAdminMapper;
import com.compute.rental.security.IdentityType;
import com.compute.rental.security.jwt.JwtTokenProvider;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock
    private SysAdminMapper sysAdminMapper;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AdminLogService adminLogService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        var configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), SysAdmin.class);
    }

    @Test
    void enabledAdminCanLoginWithValidPassword() {
        var encoder = new BCryptPasswordEncoder();
        var service = new AdminAuthService(sysAdminMapper, encoder, jwtTokenProvider, adminLogService);
        var admin = admin(1L, CommonStatus.ENABLED.value(), encoder.encode("secret"));
        when(sysAdminMapper.selectOne(any(Wrapper.class))).thenReturn(admin);
        when(jwtTokenProvider.createAccessToken(1L, "admin", "SUPER_ADMIN", IdentityType.ADMIN)).thenReturn("TOKEN");

        var response = service.login(new AdminLoginRequest("admin", "secret"), "127.0.0.1");

        assertThat(response.adminAccessToken()).isEqualTo("TOKEN");
        assertThat(response.admin().adminId()).isEqualTo(1L);
        verify(sysAdminMapper).update(any(), any(Wrapper.class));
        verify(adminLogService).log(eq(1L), eq(AdminLogService.ADMIN_LOGIN_SUCCESS), eq("sys_admin"),
                eq(1L), any(), any(), any(), eq("127.0.0.1"));
    }

    @Test
    void badPasswordShouldFailAndWriteAdminLog() {
        var encoder = new BCryptPasswordEncoder();
        var service = new AdminAuthService(sysAdminMapper, encoder, jwtTokenProvider, adminLogService);
        var admin = admin(1L, CommonStatus.ENABLED.value(), encoder.encode("secret"));
        when(sysAdminMapper.selectOne(any(Wrapper.class))).thenReturn(admin);

        assertThatThrownBy(() -> service.login(new AdminLoginRequest("admin", "bad"), "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ADMIN_BAD_CREDENTIALS);
        verify(adminLogService).log(eq(1L), eq(AdminLogService.ADMIN_LOGIN_FAIL), eq("sys_admin"),
                eq(1L), any(), any(), eq("Bad credentials"), eq("127.0.0.1"));
    }

    @Test
    void disabledAdminCannotLogin() {
        var encoder = new BCryptPasswordEncoder();
        var service = new AdminAuthService(sysAdminMapper, encoder, jwtTokenProvider, adminLogService);
        when(sysAdminMapper.selectOne(any(Wrapper.class))).thenReturn(admin(1L, 0, encoder.encode("secret")));

        assertThatThrownBy(() -> service.login(new AdminLoginRequest("admin", "secret"), "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ADMIN_DISABLED);
    }

    private SysAdmin admin(Long id, Integer status, String passwordHash) {
        var admin = new SysAdmin();
        admin.setId(id);
        admin.setUserName("admin");
        admin.setPasswordHash(passwordHash);
        admin.setRole("SUPER_ADMIN");
        admin.setStatus(status);
        return admin;
    }
}
