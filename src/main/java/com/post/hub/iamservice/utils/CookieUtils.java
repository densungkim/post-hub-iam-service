package com.post.hub.iamservice.utils;

import org.springframework.http.ResponseCookie;

import java.time.Duration;

public class CookieUtils {
    public static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    public static final String REFRESH_TOKEN = "REFRESH_TOKEN";

    public static String accessCookie(String token, Duration ttl) {
        return ResponseCookie.from(ACCESS_TOKEN, token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(ttl)
                .sameSite("Lax")
                .build()
                .toString();
    }

    public static String refreshCookie(String token, Duration ttl) {
        return ResponseCookie.from(REFRESH_TOKEN, token)
                .httpOnly(true)
                .secure(true)
                .path("/auth/refresh/token")
                .maxAge(ttl)
                .sameSite("Strict")
                .build()
                .toString();
    }

    public static String deleteAccessCookie() {
        return ResponseCookie.from(ACCESS_TOKEN, "")
                .httpOnly(true).secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build().toString();
    }

    public static String deleteRefreshCookie() {
        return ResponseCookie.from(REFRESH_TOKEN, "")
                .httpOnly(true).secure(true)
                .path("/auth/refresh/token")
                .maxAge(0)
                .sameSite("Strict")
                .build().toString();
    }
}
