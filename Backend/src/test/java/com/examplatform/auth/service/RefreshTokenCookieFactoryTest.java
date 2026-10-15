package com.examplatform.auth.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;

class RefreshTokenCookieFactoryTest {

    @Test
    void productionProfileCannotStartWithInsecureRefreshCookies() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles("prod");

        assertThrows(
                IllegalStateException.class,
                () -> new RefreshTokenCookieFactory(false, environment));
    }

    @Test
    void productionRefreshCookieIsSecureAndHttpOnly() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles("prod");
        RefreshTokenCookieFactory factory =
                new RefreshTokenCookieFactory(true, environment);

        String cookie = factory.create("opaque-test-value").toString();

        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Lax"));
        assertTrue(cookie.contains("Path=/api/auth"));
    }
}
