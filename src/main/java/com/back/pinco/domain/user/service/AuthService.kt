package com.back.pinco.domain.user.service

import com.back.pinco.domain.user.entity.User
import com.back.pinco.global.exception.ErrorCode
import com.back.pinco.global.exception.ServiceException
import com.back.pinco.global.security.JwtTokenProvider
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.RequiredArgsConstructor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.lang.Nullable
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils
import java.util.*
import java.util.concurrent.TimeUnit

@Service
@RequiredArgsConstructor
class AuthService(
    private val jwttokenProvider: JwtTokenProvider,
    private val mailService: MailService,
    private val redisTemplate: StringRedisTemplate,
    @Autowired(required = false)
    @Nullable
    private val refreshTokenStore: RefreshTokenStore? = null,
    @Autowired(required = false)
    @Nullable
    private val tokenBlacklistService: TokenBlacklistService? = null
) {
    // Redis 키 prefix
    companion object {
        private const val REDIS_KEY_PREFIX = "email:verification:code:"
        private const val VERIFICATION_CODE_EXPIRE_MINUTES = 10L
    }
    
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

    // ===================== 인증코드 관리 =====================
    @Transactional
    fun sendVerificationCode(email: String): String {
        // 이메일 형식 검증
        if (email.isBlank() || !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex())) {
            throw ServiceException(ErrorCode.INVALID_EMAIL_FORMAT)
        }
        
        // 인증코드 생성 (이메일 발송 전에 생성)
        val verificationCode = mailService.generateVerificationCode()
        
        // 이메일 발송 시도 (실패해도 인증코드는 Redis에 저장)
        try {
            mailService.sendVerificationEmail(email, verificationCode)
        } catch (e: Exception) {
            // 이메일 발송 실패해도 인증코드는 Redis에 저장하여 테스트 가능
            // 실제 운영 환경에서는 이메일 발송 실패를 로그로 기록
            e.printStackTrace()
        }
        
        val redisKey = "$REDIS_KEY_PREFIX$email"
        
        // Redis에 인증코드 저장 (10분 만료)
        try {
            redisTemplate.opsForValue().set(redisKey, verificationCode, VERIFICATION_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES)
        } catch (e: Exception) {
            // Redis 연결 실패 시 상세 로그 출력 (개발자용)
            e.printStackTrace()
            // 사용자에게는 일반적인 서버 오류 메시지 반환 (내부 인프라 문제는 사용자에게 노출하지 않음)
            throw ServiceException(ErrorCode.INTERNAL_SERVER_ERROR)
        }
        
        return verificationCode
    }

    @Transactional
    fun verifyVerificationCode(email: String, code: String) {
        val redisKey = "$REDIS_KEY_PREFIX$email"
        
        val storedCode = redisTemplate.opsForValue().get(redisKey)
            ?: throw ServiceException(ErrorCode.VERIFICATION_CODE_NOT_FOUND)
        
        // 인증코드 일치 여부 체크
        if (storedCode != code) {
            throw ServiceException(ErrorCode.VERIFICATION_CODE_NOT_MATCH)
        }
        
        // 검증 완료 후 인증코드 삭제
        redisTemplate.delete(redisKey)
    }

    // ========== 선택: 인터페이스 정의(프로젝트에 없으면 아래 두 개를 추가) ==========
    interface RefreshTokenStore {
        fun save(customerId: Long?, refreshToken: String?, ttlMillis: Long)
        fun findByCustomerId(customerId: Long?): String?
        fun deleteByCustomerId(customerId: Long?)
        fun deleteByToken(refreshToken: String?)
    }

    interface TokenBlacklistService {
        fun blacklist(accessToken: String?, ttlMillis: Long)
        fun isBlacklisted(accessToken: String?): Boolean
    }
}
