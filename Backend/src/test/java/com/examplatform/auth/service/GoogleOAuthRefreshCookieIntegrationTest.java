package com.examplatform.auth.service;

import com.examplatform.auth.entity.RefreshToken;
import com.examplatform.auth.repository.RefreshTokenRepository;
import com.examplatform.user.entity.User;
import com.examplatform.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class GoogleOAuthRefreshCookieIntegrationTest {

    @Autowired
    private GoogleAuthenticationSuccessHandler successHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void successfulGoogleCallbackPersistsRefreshTokenAndReturnsCookie()
            throws Exception {
        String subject = "test-google-" + UUID.randomUUID();
        String email = "google-" + UUID.randomUUID() + "@example.test";
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getSubject()).thenReturn(subject);
        when(oidcUser.getClaimAsString("email")).thenReturn(email);
        when(oidcUser.getClaimAsString("name")).thenReturn("Google Test Student");
        when(oidcUser.getClaimAsBoolean("email_verified")).thenReturn(true);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oidcUser);

        MockHttpServletResponse response = new MockHttpServletResponse();
        successHandler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                response,
                authentication);

        assertEquals(HttpServletResponse.SC_FOUND, response.getStatus());
        assertEquals(1, response.getHeaders(HttpHeaders.SET_COOKIE).size());
        assertEquals(
                "http://localhost:5173/auth/callback?oauth=success",
                response.getRedirectedUrl());
        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        assertTrue(setCookie.startsWith("refresh_token="));
        assertTrue(setCookie.contains("Path=/api/auth"));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("SameSite=Lax"));
        assertFalse(setCookie.contains("; Secure"));
        assertFalse(response.getRedirectedUrl().contains("accessToken"));
        assertFalse(response.getRedirectedUrl().contains("refresh_token"));

        String rawToken = setCookie.substring(
                "refresh_token=".length(),
                setCookie.indexOf(';'));
        String tokenHash = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(rawToken.getBytes(StandardCharsets.UTF_8)));

        User savedUser = userRepository.findByGoogleSubject(subject)
                .orElseThrow();
        assertEquals(email, savedUser.getEmail());
        assertEquals("STUDENT", savedUser.getRole().name());
        RefreshToken savedRefreshToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow();
        assertEquals(savedUser.getId(), savedRefreshToken.getUser().getId());
        assertFalse(savedRefreshToken.isRevoked());
    }
}
