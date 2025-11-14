package com.back.pinco.global.rq

import com.back.pinco.domain.user.entity.User
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
                    it as? User
                }

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
    }

    fun deleteCookie(name: String) = setCookie(name, null)
}

