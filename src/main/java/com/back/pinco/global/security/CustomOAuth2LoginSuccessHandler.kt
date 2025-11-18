package com.back.pinco.global.security

import com.back.pinco.domain.user.entity.User
import com.back.pinco.domain.user.service.AuthService
import com.back.pinco.global.rq.Rq
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class CustomOAuth2LoginSuccessHandler(
    private val authService: AuthService,
    private val rq: Rq
) : AuthenticationSuccessHandler {

    private val log = LoggerFactory.getLogger(CustomOAuth2LoginSuccessHandler::class.java)

    @Throws(IOException::class, ServletException::class)
    override fun onAuthenticationSuccess(request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication) {
        // Authentication.principal이 User 또는 UserPrincipal일 수 있으므로 둘 다 처리
        val principal = authentication.principal
        val user: User? = when (principal) {
            is User -> principal
            is UserPrincipal -> principal.user
            else -> null
        }

        if (user != null) {
            val accessToken = authService.genAccessToken(user)
            val refreshToken = authService.genRefreshToken(user)

            // Set cookies via Rq (추가로 디버그 헤더 및 로그)
            rq.setCookie("accessToken", accessToken)
            rq.setCookie("refreshToken", refreshToken)

            log.info("OAuth login success for userId=${user.id}, set cookies (accessToken, refreshToken)")

            // 디버그: 브라우저 Network에서 확인하기 쉽도록 플래그 헤더 추가
            response.addHeader("X-Auth-Cookie-Set", "true")
        } else {
            log.warn("OAuth login success but principal is not User or UserPrincipal: ${authentication.principal}")
            response.addHeader("X-Auth-Cookie-Set", "false")
        }

        // 프론트 엔드로 리다이렉트
        response.sendRedirect("http://localhost:3000/home")
    }
}