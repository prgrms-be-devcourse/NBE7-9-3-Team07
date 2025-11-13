package com.back.pinco.domain.user.service;

import com.back.pinco.domain.user.entity.User;
import com.back.pinco.global.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwttokenProvider;

    // ===== 선택 기능(있으면 사용, 없으면 무시) =====
    @Autowired(required = false) @Nullable
    private RefreshTokenStore refreshTokenStore;  // 예: DB/Redis에 refresh 저장 시

    @Autowired(required = false) @Nullable
    private TokenBlacklistService tokenBlacklistService; // 예: Redis 블랙리스트

    public String genAccessToken(User user) {
        return jwttokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getUserName());
    }

    public String genRefreshToken(User user) {
        return jwttokenProvider.generateRefreshToken(user.getId());
    }

    public boolean validateToken(String token) {
        return jwttokenProvider.isValid(token);
    }

    public Map<String, Object> parseToken(String token) {
        return jwttokenProvider.payloadOrNull(token);
    }

    public void logout(HttpServletRequest req, HttpServletResponse res) {
        String accessToken  = resolveAccessToken(req);
        String refreshToken = resolveRefreshToken(req);

        if (refreshTokenStore != null && StringUtils.hasText(refreshToken)) {
            refreshTokenStore.deleteByToken(refreshToken);
        }

        // (선택) access 블랙리스트
        if (tokenBlacklistService != null && StringUtils.hasText(accessToken) && jwttokenProvider.isValid(accessToken)) {
            long remainMs = jwttokenProvider.getRemainingValidityMillis(accessToken);
            if (remainMs > 0) tokenBlacklistService.blacklist(accessToken, remainMs);
        }

        // 쿠키 만료(발급 시 사용한 이름/경로/도메인 정책과 일치시켜야 함)
        expireCookie(res, "accessToken", "/");
        expireCookie(res, "refreshToken", "/");
        expireCookie(res, "apiKey", "/"); // 쓰고 있다면 함께 만료
    }

    // ===================== 헬퍼 =====================

    private String resolveAccessToken(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        if (StringUtils.hasText(h) && h.startsWith("Bearer ")) return h.substring(7);
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) if ("accessToken".equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private String resolveRefreshToken(HttpServletRequest req) {
        String rt = req.getHeader("X-Refresh-Token");
        if (StringUtils.hasText(rt)) return rt;
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) if ("refreshToken".equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private void expireCookie(HttpServletResponse res, String name, String path) {
        Cookie c = new Cookie(name, "");
        c.setHttpOnly(true);
        // c.setSecure(true); // HTTPS만 사용한다면 주석 해제 권장
        c.setPath(path == null ? "/" : path);
        c.setMaxAge(0); // 즉시 만료
        res.addCookie(c);
        // SameSite, Domain이 필요하면 Response Header로 추가
    }

    // ========== 선택: 인터페이스 정의(프로젝트에 없으면 아래 두 개를 추가) ==========
    public interface RefreshTokenStore {
        void save(Long customerId, String refreshToken, long ttlMillis);
        Optional<String> findByCustomerId(Long customerId);
        void deleteByCustomerId(Long customerId);
        void deleteByToken(String refreshToken);
    }

    public interface TokenBlacklistService {
        void blacklist(String accessToken, long ttlMillis);
        boolean isBlacklisted(String accessToken);
    }
}
