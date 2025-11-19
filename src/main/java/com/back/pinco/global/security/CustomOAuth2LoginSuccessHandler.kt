package com.back.pinco.global.security

import com.back.pinco.domain.user.service.AuthService
import com.back.pinco.domain.user.service.UserService
import com.back.pinco.global.rq.Rq
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class CustomOAuth2LoginSuccessHandler(
    private val userService: UserService,
    private val authService: AuthService,
    private val rq: Rq
) : AuthenticationSuccessHandler {

    @Throws(IOException::class, ServletException::class)
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val user = rq.getActorOrNull()

        val apiKey = userService.ensureApiKey(user)
        val accessToken = authService.genAccessToken(user)
        val refreshToken = authService.genRefreshToken(user)

        rq.setCookie("apiKey", apiKey)
        rq.setCookie("accessToken", accessToken)


        response.sendRedirect("http://localhost:3000/home")
    }
}
