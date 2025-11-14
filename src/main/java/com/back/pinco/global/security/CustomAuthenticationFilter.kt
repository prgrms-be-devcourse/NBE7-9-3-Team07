package com.back.pinco.global.security;

import com.back.pinco.domain.user.entity.User;
import com.back.pinco.domain.user.service.UserService;
import com.back.pinco.global.exception.ErrorCode;
import com.back.pinco.global.exception.ServiceException;
import com.back.pinco.global.rq.Rq;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFilter extends OncePerRequestFilter {

    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final Rq rq;

    private static final List<String> PERMIT_PATHS = List.of(
            "/api/user/join",
            "/api/user/login",
            "/api/user/reissue"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        final String uri = req.getRequestURI();
        final String method = req.getMethod();

        // Preflight 요청은 빠르게 통과
        if ("OPTIONS".equalsIgnoreCase(method)) {
            chain.doFilter(req, res);
            return;
        }

        // /api/pins 와 /api/pins/** GET은 공개
        if ("GET".equalsIgnoreCase(method) && (uri.equals("/api/pins") || uri.startsWith("/api/pins/"))) {
            chain.doFilter(req, res);
            return;
        }

        // /api/* 가 아니거나 공개 경로면 통과
        if (!uri.startsWith("/api/") || PERMIT_PATHS.stream().anyMatch(uri::equals)) {
            chain.doFilter(req, res);
            return;
        }

        // 인증 정보 추출 (Authorization: Bearer <apiKey> <accessToken> + 헤더/쿠키 fallback)
        String apiKey = rq.getHeader("X-API-Key", "");
        String accessToken = "";

        String authHeader = rq.getHeader("Authorization", "");
        if (!authHeader.isBlank()) {
            if (!authHeader.startsWith("Bearer ")) {
                write401(res, ErrorCode.INVALID_ACCESS_TOKEN);
                return;
            }
            String[] bits = authHeader.split(" ", 3); // Bearer, apiKey, access
            if (bits.length >= 2 && apiKey.isBlank()) apiKey = bits[1];
            if (bits.length == 3) accessToken = bits[2];
        }

        if (apiKey.isBlank()) apiKey = rq.getHeader("apiKey", ""); // 혹시 다른 클라이언트 호환
        if (apiKey.isBlank()) apiKey = rq.getCookieValue("apiKey", "");
        if (accessToken.isBlank()) accessToken = rq.getHeader("accessToken", "");
        if (accessToken.isBlank()) accessToken = rq.getCookieValue("accessToken", "");

        boolean hasApiKey = !apiKey.isBlank();
        boolean hasAccess = !accessToken.isBlank();

        // 인증 수단 전혀 없으면 익명 통과(정책 유지)
        if (!hasApiKey && !hasAccess) {
            chain.doFilter(req, res);
            return;
        }

        // access 토큰 검사 → 유저
        User user = null;
        boolean accessValid = false;

        if (hasAccess && tokenProvider.isValid(accessToken)) {
            Map<String, Object> payload = tokenProvider.payloadOrNull(accessToken);
            if (payload != null && payload.get("id") instanceof Number) {
                long id = ((Number) payload.get("id")).longValue();
                Optional<User> u = userService.findByIdOptional(id);
                if (u.isPresent()) {
                    user = u.get();
                    accessValid = true;
                }
            }
        }

        // 토큰이 없거나 무효면 apiKey로 대체 인증
        if (user == null && hasApiKey) {
            try {
                user = userService.findByApiKey(apiKey);
            } catch (ServiceException e) {
                write401(res, ErrorCode.INVALID_API_KEY);
                return;
            }
        }

        // 결국 유저를 못 찾으면 401
        if (user == null) {
            write401(res, ErrorCode.INVALID_ACCESS_TOKEN);
            return;
        }

        // 토큰이 있었는데 무효였다면, apiKey가 유효한 경우 새 access 토큰 재발급
        if (hasAccess && !accessValid && hasApiKey) {
            String newAccess = userService.genAccessToken(user);
            rq.setCookie("accessToken", newAccess);
            rq.setHeader("accessToken", newAccess);
        }

        // SecurityContext 주입
        var auth = new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(req, res);
    }

    private void write401(HttpServletResponse res, ErrorCode ec) throws IOException {
        if (res.isCommitted()) return;
        res.setStatus(ec.getStatus().value());
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("""
                {"errorCode":"%s","msg":"%s"}
                """.formatted(ec.getCode(), ec.getMessage()));
        res.getWriter().flush();
    }
}
