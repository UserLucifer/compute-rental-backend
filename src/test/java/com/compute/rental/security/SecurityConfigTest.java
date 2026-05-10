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
import com.compute.rental.modules.system.entity.SysAdmin;
import com.compute.rental.modules.system.mapper.SysAdminMapper;
import com.compute.rental.modules.system.service.AdminAuthService;
import com.compute.rental.modules.system.service.AdminLogService;
import com.compute.rental.modules.user.entity.AppUser;
import com.compute.rental.modules.user.mapper.AppUserMapper;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = SecurityConfigTest.TestApplication.class)
@AutoConfigureMockMvc
class SecurityConfigTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({AdminAuthController.class, PublicEndpointController.class, SecurityConfig.class, JwtAuthenticationFilter.class})
    static class TestApplication {
    }

    @RestController
    static class PublicEndpointController {

        @GetMapping({
                "/api/regions",
                "/api/gpu-models",
                "/api/products",
                "/api/products/{productCode}",
                "/api/ai-models",
                "/api/rental-cycle-rules",
                "/api/system/enums"
        })
        String publicGet() {
            return "ok";
        }

        @PostMapping("/api/rental/estimate")
        String publicEstimate(@RequestBody(required = false) String ignored) {
            return "ok";
        }

        @PostMapping("/api/products")
        String protectedCatalogMutation(@RequestBody(required = false) String ignored) {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminAuthService adminAuthService;

    @MockBean
    private AdminLogService adminLogService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AppUserMapper appUserMapper;

    @MockBean
    private SysAdminMapper sysAdminMapper;

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
        when(sysAdminMapper.selectById(1L)).thenReturn(admin(1L));
        when(adminAuthService.me(1L)).thenReturn(new AdminMeResponse(1L, "admin", 1, "SUPER_ADMIN"));

        mockMvc.perform(get("/api/admin/auth/me")
                        .header("Authorization", "Bearer TOKEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userName").value("admin"));
    }

    @Test
    void publicCatalogAndEnumEndpointsAreAnonymous() throws Exception {
        var publicGetEndpoints = new String[] {
                "/api/regions",
                "/api/gpu-models",
                "/api/products",
                "/api/products/RTX4090-BJ",
                "/api/ai-models",
                "/api/rental-cycle-rules",
                "/api/system/enums"
        };

        for (var endpoint : publicGetEndpoints) {
            mockMvc.perform(get(endpoint).param("language", "en-US"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/rental/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void catalogMutationStillRequiresUserToken() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.LOGIN_REQUIRED.code()));
    }

    @Test
    void disabledUserTokenShouldBeRejectedOnProtectedEndpoint() throws Exception {
        var principal = new JwtPrincipal(10L, "U001", "USER", IdentityType.USER.name());
        var disabled = new AppUser();
        disabled.setId(10L);
        disabled.setStatus(0);
        when(jwtTokenProvider.parse("USER_TOKEN")).thenReturn(principal);
        when(appUserMapper.selectById(10L)).thenReturn(disabled);

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer USER_TOKEN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.LOGIN_REQUIRED.code()));
    }

    @Test
    void enabledUserTokenShouldAccessUserEndpoint() throws Exception {
        var principal = new JwtPrincipal(10L, "U001", "USER", IdentityType.USER.name());
        var enabled = new AppUser();
        enabled.setId(10L);
        enabled.setStatus(1);
        when(jwtTokenProvider.parse("USER_TOKEN")).thenReturn(principal);
        when(appUserMapper.selectById(10L)).thenReturn(enabled);

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer USER_TOKEN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    private SysAdmin admin(Long id) {
        var admin = new SysAdmin();
        admin.setId(id);
        admin.setStatus(1);
        return admin;
    }
}
