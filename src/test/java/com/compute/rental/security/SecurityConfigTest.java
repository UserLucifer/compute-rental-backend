package com.compute.rental.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.compute.rental.modules.system.controller.AdminAuthController;
import com.compute.rental.modules.system.dto.AdminLoginResponse;
import com.compute.rental.modules.system.dto.AdminMeResponse;
import com.compute.rental.modules.system.service.AdminAuthService;
import com.compute.rental.modules.system.service.AdminLogService;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.security.jwt.JwtAuthenticationFilter;
import com.compute.rental.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = SecurityConfigTest.TestApplication.class)
@AutoConfigureMockMvc
class SecurityConfigTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({AdminAuthController.class, SecurityConfig.class, JwtAuthenticationFilter.class})
    static class TestApplication {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminAuthService adminAuthService;

    @MockBean
    private AdminLogService adminLogService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void adminLoginIsPublic() throws Exception {
        var admin = new AdminMeResponse(1L, "admin", 1, "SUPER_ADMIN");
        when(adminAuthService.login(any(), any())).thenReturn(new AdminLoginResponse("TOKEN", admin));

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adminAccessToken").value("TOKEN"))
                .andExpect(jsonPath("$.data.admin.userName").value("admin"));
    }

    @Test
    void adminMeRequiresAdminToken() throws Exception {
        mockMvc.perform(get("/api/admin/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.LOGIN_REQUIRED.code()))
                .andExpect(jsonPath("$.message").value(ErrorCode.LOGIN_REQUIRED.message()));

        var principal = new JwtPrincipal(1L, "admin", "SUPER_ADMIN", IdentityType.ADMIN.name());
        when(jwtTokenProvider.parse("TOKEN")).thenReturn(principal);
        when(adminAuthService.me(1L)).thenReturn(new AdminMeResponse(1L, "admin", 1, "SUPER_ADMIN"));

        mockMvc.perform(get("/api/admin/auth/me")
                        .header("Authorization", "Bearer TOKEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userName").value("admin"));
    }
}
