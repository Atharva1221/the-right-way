package com.examplatform.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.time.Duration;

@Component
public class RefreshTokenCookieFactory {

    private static final String COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/api/auth";

    private final boolean secure;

    public RefreshTokenCookieFactory(
            @Value("${auth.refresh-cookie.secure:false}") boolean secure,
            Environment environment) {
        if (environment.acceptsProfiles(Profiles.of("prod")) && !secure) {
            throw new IllegalStateException(
                    "Refresh-token cookies must be Secure in the production profile");
        }
        this.secure = secure;
    }

    public ResponseCookie create(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(Duration.ofHours(3))
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }
}
