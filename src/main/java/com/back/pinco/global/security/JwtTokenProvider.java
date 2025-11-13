package com.back.pinco.global.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private final Key key;                 // 서명/검증용 키(한 번 생성 후 재사용)
    private final long accessExpMs;        // 액세스 토큰 만료(ms)
    private final long refreshExpMs;       // 리프레시 토큰 만료(ms)

    public JwtTokenProvider(
            @Value("${custom.jwt.secret}") String secret,
            @Value("${custom.jwt.accessExpireSeconds}") long accessExpireSeconds,
            @Value("${custom.jwt.refreshExpireSeconds}") long refreshExpireSeconds
    ) {
        // HS256은 32바이트 이상 권장
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpMs = accessExpireSeconds * 1000L;
        this.refreshExpMs = refreshExpireSeconds * 1000L;
    }

    // 토큰 발급
    public String generateAccessToken(Long id, String email, String userName) {
        return build(accessExpMs, Map.of(
                "id", id,
                "email", email,
                "userName", userName,
                "role", "ROLE_USER"
        ));
    }

    public String generateRefreshToken(Long id) {
        return build(refreshExpMs, Map.of("id", id));
    }

    private String build(long expMs, Map<String, Object> claims) {
        Date now = new Date();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(claims.get("id")))
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 토큰 검증, 파싱
    public boolean isValid(String token) {
        try {
            parser().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.valueOf(parser().parseClaimsJws(token).getBody().getSubject());
    }

    public Map<String, Object> payloadOrNull(String token) {
        try {
            Claims c = parser().parseClaimsJws(token).getBody();
            return Map.of(
                    "id", Long.valueOf(c.getSubject()),
                    "email", c.get("email", String.class),
                    "userName", c.get("userName", String.class),
                    "role", c.getOrDefault("role", "ROLE_USER")
            );
        } catch (Exception e) {
            return null;
        }
    }

    private JwtParser parser() {
        return Jwts.parserBuilder().setSigningKey(key).build();
    }

    // 남은 토큰 유효 시간
    public long getRemainingValidityMillis(String token) {
        try {
            Date exp = parser().parseClaimsJws(token).getBody().getExpiration();
            long now = System.currentTimeMillis();
            return (exp == null || exp.getTime() <= now) ? 0L : (exp.getTime() - now);
        } catch (Exception e) {
            return 0L;
        }
    }
}



