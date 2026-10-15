package com.examplatform.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class SecurityConfigTest {

    @Test
    void acceptsAListOfExplicitHttpOrigins() {
        assertEquals(
                List.of(
                        "https://app.example.test",
                        "http://localhost:5173"),
                SecurityConfig.parseAllowedOrigins(
                        " https://app.example.test/ , http://localhost:5173 "));
    }

    @Test
    void rejectsWildcardOriginsWhenCredentialsAreEnabled() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SecurityConfig.parseAllowedOrigins("*"));
        assertThrows(
                IllegalArgumentException.class,
                () -> SecurityConfig.parseAllowedOrigins(
                        "https://*.example.test"));
    }

    @Test
    void rejectsOriginsContainingPathsOrNonHttpSchemes() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SecurityConfig.parseAllowedOrigins(
                        "https://app.example.test/path"));
        assertThrows(
                IllegalArgumentException.class,
                () -> SecurityConfig.parseAllowedOrigins(
                        "javascript:alert(1)"));
    }

    @Test
    void productionCorsOriginsMustUseHttps() {
        assertEquals(
                List.of("https://app.example.test"),
                SecurityConfig.parseAllowedOrigins(
                        "https://app.example.test",
                        true));
        assertThrows(
                IllegalArgumentException.class,
                () -> SecurityConfig.parseAllowedOrigins(
                        "http://app.example.test",
                        true));
    }

    @Test
    void productionFrontendUrlMustBeAnHttpsOrigin() {
        assertEquals(
                "https://app.example.test",
                SecurityConfig.normalizeFrontendUrl(
                        "https://app.example.test/",
                        true));
        assertThrows(
                IllegalArgumentException.class,
                () -> SecurityConfig.normalizeFrontendUrl(
                        "http://app.example.test",
                        true));
        assertThrows(
                IllegalArgumentException.class,
                () -> SecurityConfig.normalizeFrontendUrl(
                        "https://app.example.test/path",
                        true));
    }
}
