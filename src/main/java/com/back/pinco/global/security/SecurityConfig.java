package com.back.pinco.global.security;

import com.back.pinco.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationFilter customAuthenticationFilter; // 통합 필터

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 공개 API
                        .requestMatchers("/api/user/join", "/api/user/login", "/api/user/reissue").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/pins/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tags/**").permitAll()

                        // 그 외 /api/** 는 인증 필요
                        .requestMatchers("/api/**").authenticated()

                        // Swagger
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // 나머지는 전부 허용
                        .anyRequest().permitAll()
                )

                // 🔥 추가된 부분 (최소 수정)
                .exceptionHandling(ex -> ex
                        // 인증 실패 (로그인 안함, 잘못된 apiKey 등) → 401로 통일
                        .authenticationEntryPoint(this::handleAuthEntryPoint)

                        // 인가 실패 (ROLE 부족 등) → 403
                        .accessDeniedHandler(this::handleAccessDenied)
                )

                .addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 🔥 401 Unauthorized 처리
    private void handleAuthEntryPoint(HttpServletRequest request,
                                      HttpServletResponse response,
                                      AuthenticationException authException) throws IOException {

        ErrorCode code = ErrorCode.AUTH_REQUIRED; // 기본적으로 로그인 필요

        response.setStatus(code.getStatus().value()); // 401
        response.setContentType("application/json;charset=UTF-8");

        // ObjectMapper 안 쓰고 직접 JSON 작성
        response.getWriter().write("""
            {
              "errorCode": "%s",
              "msg": "%s",
              "data": null
            }
            """.formatted(code.getCode(), code.getMessage()));
    }

    // 🔥 403 Forbidden 처리 (권한 부족)
    private void handleAccessDenied(HttpServletRequest request,
                                    HttpServletResponse response,
                                    org.springframework.security.access.AccessDeniedException ex) throws IOException {

        // ErrorCode.ACCESS_DENIED 만들어두면 더 좋음!
        ErrorCode code = ErrorCode.ACCESS_DENIED;  // 없으면 하나 추가해야함

        response.setStatus(code.getStatus().value()); // 403
        response.setContentType("application/json;charset=UTF-8");

        response.getWriter().write("""
            {
              "errorCode": "%s",
              "msg": "%s",
              "data": null
            }
            """.formatted(code.getCode(), code.getMessage()));
    }
}
