package com.back.pinco.global.security

import com.back.pinco.global.exception.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import java.io.IOException

@Configuration
class SecurityConfig (
    private val customAuthenticationFilter: CustomAuthenticationFilter
) {
    @Bean
    @Throws(Exception::class)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf: CsrfConfigurer<HttpSecurity> -> csrf.disable() }
            .cors(Customizer.withDefaults())
            .sessionManagement { sm: SessionManagementConfigurer<HttpSecurity?> ->
                sm.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            }
            .authorizeHttpRequests { auth ->
                    auth // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // 공개 API
                        .requestMatchers("/api/user/join", "/api/user/login", "/api/user/reissue").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/pins/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tags/**").permitAll() // 그 외 /api/** 는 인증 필요
                        .requestMatchers("/api/**").authenticated() // Swagger
                        .requestMatchers(
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/v3/api-docs/**",
                            "/swagger-resources/**",
                            "/webjars/**"
                        ).permitAll() // 나머지는 전부 허용

                        .anyRequest().permitAll()
                }

            .exceptionHandling { ex: ExceptionHandlingConfigurer<HttpSecurity?> ->
                ex // 인증 실패 (로그인 안함, 잘못된 apiKey 등) → 401로 통일
                    .authenticationEntryPoint { request: HttpServletRequest, response: HttpServletResponse, authException: AuthenticationException ->
                        this.handleAuthEntryPoint(
                            request,
                            response,
                            authException
                        )
                    } // 인가 실패 (ROLE 부족 등) → 403

                    .accessDeniedHandler { request: HttpServletRequest, response: HttpServletResponse, ex: AccessDeniedException ->
                        this.handleAccessDenied(
                            request,
                            response,
                            ex
                        )
                    }
            }

            .addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Throws(IOException::class)
    private fun handleAuthEntryPoint(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        val code = ErrorCode.AUTH_REQUIRED // 기본적으로 로그인 필요

        response.status = code.status.value() // 401
        response.contentType = "application/json;charset=UTF-8"

        // ObjectMapper 안 쓰고 직접 JSON 작성
        response.writer.write(
            """
            {
              "errorCode": "%s",
              "msg": "%s",
              "data": null
            }
            
            """.trimIndent().formatted(code.code, code.message)
        )
    }

    @Throws(IOException::class)
    private fun handleAccessDenied(
        request: HttpServletRequest,
        response: HttpServletResponse,
        ex: AccessDeniedException
    ) {
        val code = ErrorCode.ACCESS_DENIED

        response.status = code.status.value() // 403
        response.contentType = "application/json;charset=UTF-8"

        response.writer.write(
            """
            {
              "errorCode": "%s",
              "msg": "%s",
              "data": null
            }
            
            """.trimIndent().formatted(code.code, code.message)
        )
    }
}
