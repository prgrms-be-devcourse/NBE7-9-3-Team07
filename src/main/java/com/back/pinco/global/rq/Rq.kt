package com.back.pinco.global.rq

import com.back.pinco.domain.user.entity.User
import com.back.pinco.global.exception.ErrorCode
import com.back.pinco.global.exception.ServiceException
import com.back.pinco.global.security.UserPrincipal
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class Rq (
    private val request: HttpServletRequest,
    private val response: HttpServletResponse
){

    val actor: User?
        get() =
            SecurityContextHolder
                .getContext()
                ?.authentication
                ?.principal
                ?.let{
                    when (it) {
                        is User -> it
                        is UserPrincipal -> it.user
                        else -> null
                    }
                }

    // actor 반환
    fun getActorOrNull(): User =
        actor ?: throw ServiceException(ErrorCode.AUTH_REQUIRED)

    // 인증된 사용자 ID를 반환, 없으면 예외 발생
    fun getActorIdOrThrow(): Long =
        actor?.id ?: throw ServiceException(ErrorCode.AUTH_REQUIRED)

    fun getHeader(name: String, defaultValue: String): String =
        request.getHeader(name)?:defaultValue

    fun setHeader(name: String, value: String) =
        response.setHeader(name, value)

    fun getCookieValue(name: String, defaultValue: String): String =
        request.cookies
            ?.firstOrNull { it.name == name && it.value.isNullOrBlank().not() }
            ?.value
            ?: defaultValue

    fun setCookie(name: String, value: String?) {
        val cookie = Cookie(name, value.orEmpty()).apply {
            path = "/"
            isHttpOnly = true
            secure = request.isSecure          // 로컬 http면 false, https면 true
            setAttribute("SameSite", "Lax")

            if (value.isNullOrBlank()) {
                maxAge = 0
            }
        }

        response.addCookie(cookie)

        // 일부 서블릿 컨테이너/브라우저 환경에서 Cookie#setAttribute(SameSite) 가 적용되지 않을 수 있어
        // 명시적으로 Set-Cookie 헤더도 추가합니다 (중복 허용).
        // SameSite=None을 사용하려면 HTTPS + Secure가 필요하니 로컬 개발에서는 Lax를 사용합니다.
        val sb = StringBuilder()
        sb.append("$name=${value.orEmpty()}; Path=/")
        if (cookie.maxAge >= 0) sb.append("; Max-Age=${cookie.maxAge}")
        if (cookie.isHttpOnly) sb.append("; HttpOnly")
        if (cookie.secure) sb.append("; Secure")
        sb.append("; SameSite=Lax")

        response.addHeader("Set-Cookie", sb.toString())
    }

    fun deleteCookie(name: String) = setCookie(name, null)
}
