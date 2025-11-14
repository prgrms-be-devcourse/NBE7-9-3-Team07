package com.back.pinco.global.rq;

import com.back.pinco.domain.user.entity.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class Rq {
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public User getActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        return (principal instanceof User u) ? u : null;
    }

    public String getHeader(String name, String defaultValue) {
        String v = request.getHeader(name);
        return (v != null && !v.isBlank()) ? v : defaultValue;
    }

    public void setHeader(String name, String value) {
        response.setHeader(name, value);
    }

    public String getCookieValue(String name, String defaultValue) {
        return Optional.ofNullable(request.getCookies())
                .flatMap(cookies -> Arrays.stream(cookies)
                        .filter(c -> c.getName().equals(name))
                        .map(Cookie::getValue)
                        .filter(v -> !v.isBlank())
                        .findFirst()
                ).orElse(defaultValue);
    }

    public void setCookie(String name, String value) {
        Cookie cookie = new Cookie(name, value == null ? "" : value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());     // 로컬 http면 false, https면 true
        cookie.setAttribute("SameSite", "Lax");
        if (value == null || value.isBlank()) cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    public void deleteCookie(String name) { setCookie(name, null); }
}

