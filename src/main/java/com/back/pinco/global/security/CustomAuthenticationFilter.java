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

        // /api/pins 와 /api/pins/** 둘 다 허용
        if (method.equals("GET") && (uri.equals("/api/pins") || uri.startsWith("/api/pins/"))) {
            chain.doFilter(req, res);
            return;
        }

        // /api/* 가 아니거나 공개 경로(join, login)면 바로 통과
        if (!uri.startsWith("/api/") || PERMIT_PATHS.stream().anyMatch(uri::equals)) {
            chain.doFilter(req, res);
            return;
        }

        // 인증 정보 추출 (Authorization: Bearer <apiKey> <accessToken> 지원 + 쿠키 fallback)
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

        if (apiKey.isBlank()) apiKey = rq.getCookieValue("apiKey", "");
        if (accessToken.isBlank()) accessToken = rq.getCookieValue("accessToken", "");

        boolean hasApiKey = !apiKey.isBlank();
        boolean hasAccess = !accessToken.isBlank();

        // 인증 수단 전혀 없으면 익명 통과(정책에 맞게 유지)
        if (!hasApiKey && !hasAccess) {
            chain.doFilter(req, res);
            return;
        }

        // access 토큰 검사 → 유저
        User user = null;
        boolean accessValid = false;

        if (hasAccess && tokenProvider.isValid(accessToken)) {
            Map<String, Object> payload = tokenProvider.payloadOrNull(accessToken);
            if (payload != null) {
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



