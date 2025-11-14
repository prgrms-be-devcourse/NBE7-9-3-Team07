package com.back.pinco.global.security

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.Key
import java.util.*

@Component
class JwtTokenProvider(
    @Value("\${custom.jwt.secret}") secret: String,
    @Value("\${custom.jwt.accessExpireSeconds}") accessExpireSeconds: Long,
    @Value("\${custom.jwt.refreshExpireSeconds}") refreshExpireSeconds: Long,

) {
    // HS256은 32바이트 이상 권장
    private val key: Key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8)) // 서명/검증용 키(한 번 생성 후 재사용)
    private val accessExpMs = accessExpireSeconds * 1000L // 액세스 토큰 만료(ms)
    private val refreshExpMs = refreshExpireSeconds * 1000L // 리프레시 토큰 만료(ms)

    // 토큰 발급
    fun generateAccessToken(id: Long?, email: String?, userName: String?): String {
        return build(
            accessExpMs, java.util.Map.of<String, Any?>(
                "id", id,
                "email", email,
                "userName", userName,
                "role", "ROLE_USER"
            )
        )
    }

    fun generateRefreshToken(id: Long?): String {
        return build(refreshExpMs, java.util.Map.of<String, Any?>("id", id))
    }

    private fun build(expMs: Long, claims: Map<String, Any?>): String {
        val now = Date()
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(claims["id"].toString())
            .setIssuedAt(now)
            .setExpiration(Date(now.time + expMs))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    // 토큰 검증, 파싱
    fun isValid(token: String?): Boolean {
        try {
            parser().parseClaimsJws(token)
            return true
        } catch (e: JwtException) {
            return false
        } catch (e: IllegalArgumentException) {
            return false
        }
    }

    fun getUserId(token: String?): Long {
        return parser().parseClaimsJws(token).body.subject.toLong()
    }

    fun payloadOrNull(token: String?): Map<String, Any>? {
        try {
            val c = parser().parseClaimsJws(token).body
            return java.util.Map.of(
                "id", c.subject.toLong(),
                "email", c.get("email", String::class.java),
                "userName", c.get("userName", String::class.java),
                "role", c.getOrDefault("role", "ROLE_USER")
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun parser(): JwtParser {
        return Jwts.parserBuilder().setSigningKey(key).build()
    }

    // 남은 토큰 유효 시간
    fun getRemainingValidityMillis(token: String?): Long {
        try {
            val exp = parser().parseClaimsJws(token).body.expiration
            val now = System.currentTimeMillis()
            return if ((exp == null || exp.time <= now)) 0L else (exp.time - now)
        } catch (e: Exception) {
            return 0L
        }
    }
}



