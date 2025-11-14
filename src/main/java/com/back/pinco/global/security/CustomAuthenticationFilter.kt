package com.back.pinco.global.security

import com.back.pinco.domain.user.entity.User
import com.back.pinco.domain.user.service.UserService
import com.back.pinco.global.exception.ErrorCode
import com.back.pinco.global.exception.ServiceException
import com.back.pinco.global.rq.Rq
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

@Component
class CustomAuthenticationFilter(
    private val userService: UserService,
    private val tokenProvider: JwtTokenProvider,
    private val rq: Rq,
) : OncePerRequestFilter() {

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val uri = req.requestURI
        val method = req.method

        // Preflight 요청은 빠르게 통과
        if (method.equals("OPTIONS", ignoreCase = true)) {
            chain.doFilter(req, res)
            return
        }

        // /api/pins 와 /api/pins/** GET은 공개
        if (method.equals("GET", ignoreCase = true) && (uri == "/api/pins" || uri.startsWith("/api/pins/"))) {
            chain.doFilter(req, res)
            return
        }

        // /api/* 가 아니거나 공개 경로면 통과
        if (!uri.startsWith("/api/") || PERMIT_PATHS.contains(uri)) {
            chain.doFilter(req, res)
            return
        }

        // 인증 정보 추출 (Authorization: Bearer <apiKey> <accessToken> + 헤더/쿠키 fallback)
        var apiKey = rq.getHeader("X-API-Key", "")
        var accessToken = ""

        val authHeader = rq.getHeader("Authorization", "")
        if (authHeader.isNotBlank()) {
            if (!authHeader.startsWith("Bearer ")) {
                write401(res, ErrorCode.INVALID_ACCESS_TOKEN)
                return
            }
            val bits = authHeader.split(" ", limit = 3) // Bearer, apiKey, access
            if (bits.size >= 2 && apiKey.isBlank()) apiKey = bits[1]
            if (bits.size == 3) accessToken = bits[2]
        }

        if (apiKey.isBlank()) apiKey = rq.getHeader("apiKey", "") // 혹시 다른 클라이언트 호환

        if (apiKey.isBlank()) apiKey = rq.getCookieValue("apiKey", "")
        if (accessToken.isBlank()) accessToken = rq.getHeader("accessToken", "")
        if (accessToken.isBlank()) accessToken = rq.getCookieValue("accessToken", "")

        val hasApiKey = apiKey.isNotBlank()
        val hasAccess = accessToken.isNotBlank()

        // 인증 수단 전혀 없으면 익명 통과(정책 유지)
        if (!hasApiKey && !hasAccess) {
            chain.doFilter(req, res)
            return
        }

        // access 토큰 검사 → 유저
        var user: User? = null
        var accessValid = false

        if (hasAccess && tokenProvider.isValid(accessToken)) {
            val payload = tokenProvider.payloadOrNull(accessToken)
            val idAny = payload?.get("id")
            if (idAny is Number) {
                val id = idAny.toLong()
                val u = userService.findByIdOptional(id)
                if (u.isPresent) {
                    user = u.get()
                    accessValid = true
                }
            }
        }

        // 토큰이 없거나 무효면 apiKey로 대체 인증
        if (user == null && hasApiKey) {
            try {
                user = userService.findByApiKey(apiKey)
            } catch (e: ServiceException) {
                write401(res, ErrorCode.INVALID_API_KEY)
                return
            }
        }

        // 결국 유저를 못 찾으면 401
        if (user == null) {
            write401(res, ErrorCode.INVALID_ACCESS_TOKEN)
            return
        }

        // 토큰이 있었는데 무효였다면, apiKey가 유효한 경우 새 access 토큰 재발급
        if (hasAccess && !accessValid && hasApiKey) {
            val newAccess = userService.genAccessToken(user)
            rq.setCookie("accessToken", newAccess)
            rq.setHeader("accessToken", newAccess)
        }

        // SecurityContext 주입
        val auth = UsernamePasswordAuthenticationToken(
            user,
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER")) // Kotlin 컬렉션 사용
        )
        SecurityContextHolder.getContext().authentication = auth

        chain.doFilter(req, res)
    }

    @Throws(IOException::class)
    private fun write401(res: HttpServletResponse, ec: ErrorCode) {
        if (res.isCommitted) return
        res.status = ec.status.value()
        res.contentType = "application/json;charset=UTF-8"
        res.writer.write(
            """
                {"errorCode":"%s","msg":"%s"}
                
            """.trimIndent().formatted(ec.code, ec.message)
        )
        res.writer.flush()
    }

    companion object {
        private val PERMIT_PATHS = listOf(
            "/api/user/join",
            "/api/user/login",
            "/api/user/reissue"
        )
    }
}
