package com.compute.rental.security;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.security.jwt.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/**",
            "/api/admin/auth/login",
            "/api/blog/**",
            "/api/docs/**",
            "/api/system/health",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/doc.html",
            "/webjars/**",
            "/favicon.ico",
            "/ws/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectMapper objectMapper
    )
            throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, ex) ->
                                writeSecurityError(response, objectMapper, ErrorCode.LOGIN_REQUIRED))
                        .accessDeniedHandler((request, response, ex) ->
                                writeSecurityError(response, objectMapper, ErrorCode.FORBIDDEN)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(publicEndpointMatchers()).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().hasRole("USER")
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private AntPathRequestMatcher[] publicEndpointMatchers() {
        var matchers = new AntPathRequestMatcher[PUBLIC_ENDPOINTS.length];
        for (int i = 0; i < PUBLIC_ENDPOINTS.length; i++) {
            matchers[i] = new AntPathRequestMatcher(PUBLIC_ENDPOINTS[i]);
        }
        return matchers;
    }

    private void writeSecurityError(HttpServletResponse response, ObjectMapper objectMapper, ErrorCode errorCode)
            throws IOException {
        response.setStatus(errorCode.httpStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(errorCode));
    }
}
