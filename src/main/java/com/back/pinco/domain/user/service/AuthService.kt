package com.back.pinco.domain.user.service

import com.back.pinco.domain.user.entity.User
import com.back.pinco.global.security.JwtTokenProvider
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.RequiredArgsConstructor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.lang.Nullable
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import java.util.*

@Service
@RequiredArgsConstructor
class AuthService(
    private val jwttokenProvider: JwtTokenProvider,
    @Autowired(required = false)
    @Nullable
    private val refreshTokenStore: RefreshTokenStore? = null,
    @Autowired(required = false)
    @Nullable
    private val tokenBlacklistService: TokenBlacklistService? = null
) {
    fun genAccessToken(user: User): String =
        jwttokenProvider.generateAccessToken(user.id, user.email, user.userName)


    fun genRefreshToken(user: User): String =
        jwttokenProvider.generateRefreshToken(user.id)


    fun validateToken(token: String?): Boolean =
        jwttokenProvider.isValid(token)


    fun parseToken(token: String?): Map<String, Any>? =
        jwttokenProvider.payloadOrNull(token)


    fun logout(req: HttpServletRequest, res: HttpServletResponse) {
        val accessToken = resolveAccessToken(req)
        val refreshToken = resolveRefreshToken(req)

        if (refreshTokenStore != null && StringUtils.hasText(refreshToken)) {
            refreshTokenStore.deleteByToken(refreshToken)
        }

        // (선택) access 블랙리스트
        if (tokenBlacklistService != null && StringUtils.hasText(accessToken) && jwttokenProvider.isValid(accessToken)) {
            val remainMs = jwttokenProvider.getRemainingValidityMillis(accessToken)
            if (remainMs > 0) tokenBlacklistService.blacklist(accessToken, remainMs)
        }

        // 쿠키 만료(발급 시 사용한 이름/경로/도메인 정책과 일치시켜야 함)
        expireCookie(res, "accessToken", "/")
        expireCookie(res, "refreshToken", "/")
        expireCookie(res, "apiKey", "/") // 쓰고 있다면 함께 만료
    }

    // ===================== 헬퍼 =====================
    private fun resolveAccessToken(req: HttpServletRequest): String? {
        val h = req.getHeader("Authorization")
        if (StringUtils.hasText(h) && h.startsWith("Bearer ")) return h.substring(7)
        val cookies = req.cookies
        if (cookies != null) {
            for (c in cookies) if ("accessToken" == c.name) return c.value
        }
        return null
    }

    private fun resolveRefreshToken(req: HttpServletRequest): String? {
        val rt = req.getHeader("X-Refresh-Token")
        if (StringUtils.hasText(rt)) return rt
        val cookies = req.cookies
        if (cookies != null) {
            for (c in cookies) if ("refreshToken" == c.name) return c.value
        }
        return null
    }

    private fun expireCookie(res: HttpServletResponse, name: String, path: String?) {
        val c = Cookie(name, "")
        c.isHttpOnly = true
        // c.setSecure(true); // HTTPS만 사용한다면 주석 해제 권장
        c.path = path ?: "/"
        c.maxAge = 0 // 즉시 만료
        res.addCookie(c)
        // SameSite, Domain이 필요하면 Response Header로 추가
    }

    // ========== 선택: 인터페이스 정의(프로젝트에 없으면 아래 두 개를 추가) ==========
    interface RefreshTokenStore {
        fun save(customerId: Long?, refreshToken: String?, ttlMillis: Long)
        fun findByCustomerId(customerId: Long?): Optional<String?>?
        fun deleteByCustomerId(customerId: Long?)
        fun deleteByToken(refreshToken: String?)
    }

    interface TokenBlacklistService {
        fun blacklist(accessToken: String?, ttlMillis: Long)
        fun isBlacklisted(accessToken: String?): Boolean
    }
}
