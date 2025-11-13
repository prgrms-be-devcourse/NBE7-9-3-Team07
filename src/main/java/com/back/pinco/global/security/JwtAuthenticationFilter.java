package com.back.pinco.global.security;

import com.back.pinco.domain.user.entity.User;
import com.back.pinco.domain.user.repository.UserRepository;
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

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final Rq rq;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String uri = req.getRequestURI();

        // 공개 엔드포인트
        if (uri.startsWith("/api/user/join")
                || uri.startsWith("/api/user/login")
                || uri.startsWith("/api/user/reissue")) {
            chain.doFilter(req, res);
            return;
        }

        // /api/** 만 인증 체크
        if (!uri.startsWith("/api/")) {
            chain.doFilter(req, res);
            return;
        }

        String accessToken = null;
        String authHeader = rq.getHeader("Authorization", "");
        if (authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7).trim();
        }
        if (accessToken == null || accessToken.isBlank()) {
            accessToken = rq.getCookieValue("accessToken", "");
        }

        if (accessToken != null && !accessToken.isBlank() && tokenProvider.isValid(accessToken)) {
            Long userId = tokenProvider.getUserId(accessToken);
            userRepository.findById(userId).ifPresent(user -> {
                var auth = new UsernamePasswordAuthenticationToken(
                        user, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }

        chain.doFilter(req, res);
    }
}

