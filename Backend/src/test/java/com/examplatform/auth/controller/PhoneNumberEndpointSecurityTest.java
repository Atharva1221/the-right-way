package com.examplatform.auth.controller;

import com.examplatform.auth.dto.AuthResponse;
import com.examplatform.auth.dto.RegisterRequest;
import com.examplatform.auth.service.AuthService.LoginResult;
import com.examplatform.auth.service.AuthService;
import com.examplatform.auth.service.GoogleAuthenticationSuccessHandler;
import com.examplatform.auth.service.RefreshTokenService;
import com.examplatform.auth.entity.RefreshToken;
import com.examplatform.security.JwtService;
import com.examplatform.user.entity.User;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class PhoneNumberEndpointSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Autowired
    private GoogleAuthenticationSuccessHandler googleAuthenticationSuccessHandler;

    @Test
    void publicRegistrationCreatesOnlyStudentAccountsAndReturnsCreated() throws Exception {
        when(authService.registerStudent(any(RegisterRequest.class)))
                .thenReturn(new AuthResponse(
                        20L,
                        "New Student",
                        "new.student@example.test",
                        "STUDENT",
                        "+1 415 555 0134",
                        null));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "New Student",
                                  "email": "new.student@example.test",
                                  "phoneNumber": "+1 415 555 0134",
                                  "password": "StudentPassword123!",
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.accessToken").value((Object) null));

        verify(authService).registerStudent(argThat(request ->
                request.getEmail().equals("new.student@example.test")
                        && request.getPhoneNumber().equals("+1 415 555 0134")));
    }

    @Test
    void registrationRejectsInvalidFieldsAndDuplicateEmails() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " ",
                                  "email": "invalid",
                                  "phoneNumber": "123456",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        when(authService.registerStudent(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Email is already registered"));
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "New Student",
                                  "email": "new.student@example.test",
                                  "phoneNumber": "+1 415 555 0134",
                                  "password": "StudentPassword123!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email is already registered"));
    }

    @Test
    void currentUserResponseIncludesNullPhoneAndIncompleteState() throws Exception {
        when(authService.getUserResponse("google@example.com"))
                .thenReturn(new AuthResponse(
                        10L,
                        "Google Student",
                        "google@example.com",
                        "STUDENT",
                        null,
                        null));

        mockMvc.perform(get("/api/auth/me")
                        .with(user("google@example.com").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.profileComplete").value(false));

        verify(authService).getUserResponse("google@example.com");
    }

    @Test
    void studentCanUpdatePhoneOnlyForAuthenticatedIdentity() throws Exception {
        when(authService.updatePhoneNumber(
                "student@example.com",
                "+91 98765 43210"))
                .thenReturn(new AuthResponse(
                        11L,
                        "Student",
                        "student@example.com",
                        "STUDENT",
                        "+91 98765 43210",
                        null));

        mockMvc.perform(put("/api/auth/me/phone-number")
                        .with(user("student@example.com").roles("STUDENT"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber": " +91 98765 43210 ",
                                  "userId": 999
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("+91 98765 43210"))
                .andExpect(jsonPath("$.profileComplete").value(true));

        verify(authService).updatePhoneNumber(
                "student@example.com",
                "+91 98765 43210");
    }

    @Test
    void adminCannotUseStudentPhoneUpdateEndpoint() throws Exception {
        mockMvc.perform(put("/api/auth/me/phone-number")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"+91 98765 43210\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(authService);
    }

    @Test
    void successfulGoogleCallbackSendsHttpOnlyRefreshCookieAndAccessToken()
            throws Exception {
        OidcUser googleUser = mock(OidcUser.class);
        when(googleUser.getSubject()).thenReturn("google-subject");
        when(googleUser.getClaimAsString("email"))
                .thenReturn("google@example.com");
        when(googleUser.getClaimAsString("name")).thenReturn("Google Student");
        when(googleUser.getClaimAsBoolean("email_verified")).thenReturn(true);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(googleUser);
        when(authService.loginWithGoogle(
                "google-subject",
                "google@example.com",
                "Google Student",
                true))
                .thenReturn(new LoginResult(
                        new AuthResponse(
                                12L,
                                "Google Student",
                                "google@example.com",
                                "STUDENT",
                                null,
                                "issued-access-token"),
                        "issued-refresh-token"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        googleAuthenticationSuccessHandler.onAuthenticationSuccess(
                new MockHttpServletRequest(), response, authentication);

        assertEquals(HttpServletResponse.SC_FOUND, response.getStatus());
        assertEquals(
                "http://localhost:5173/auth/callback?oauth=success",
                response.getRedirectedUrl());
        assertEquals(1, response.getHeaders(HttpHeaders.SET_COOKIE).size());
        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(setCookie.startsWith("refresh_token=issued-refresh-token"));
        assertTrue(setCookie.contains("Path=/api/auth"));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("SameSite=Lax"));
        assertFalse(setCookie.contains("; Secure"));
        assertFalse(response.getRedirectedUrl().contains("issued-access-token"));
        assertFalse(response.getRedirectedUrl().contains("issued-refresh-token"));
        verify(authService).loginWithGoogle(
                "google-subject",
                "google@example.com",
                "Google Student",
                true);
    }

    @Test
    void refreshWithoutCookieReturnsJson401AndClearsCookie() throws Exception {
        mockMvc.perform(post("/api/auth/refresh").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value(
                        "Refresh token is missing, invalid, expired, or revoked"))
                .andExpect(result -> {
                    String setCookie = result.getResponse()
                            .getHeader(HttpHeaders.SET_COOKIE);
                    assertTrue(setCookie.contains("refresh_token="));
                    assertTrue(setCookie.contains("Max-Age=0"));
                    assertTrue(setCookie.contains("Path=/api/auth"));
                    assertTrue(setCookie.contains("HttpOnly"));
                    assertTrue(setCookie.contains("SameSite=Lax"));
                    assertFalse(setCookie.contains("; Secure"));
                });
    }

    @Test
    void invalidRefreshCookieReturns401InsteadOfRedirecting() throws Exception {
        when(refreshTokenService.validateRefreshToken("invalid"))
                .thenThrow(new IllegalArgumentException("Invalid refresh token"));

        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .cookie(new jakarta.servlet.http.Cookie(
                                "refresh_token", "invalid")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(result -> assertEquals(
                        null,
                        result.getResponse().getHeader("Location")));
    }

    @Test
    void successfulRefreshReturnsAccessTokenAndRotatesCookie() throws Exception {
        RefreshToken currentToken = mock(RefreshToken.class);
        User user = mock(User.class);
        when(currentToken.getUser()).thenReturn(user);
        when(user.getEmail()).thenReturn("student@example.com");
        when(refreshTokenService.validateRefreshToken("old-refresh"))
                .thenReturn(currentToken);
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("student@example.com")
                .password("")
                .authorities("ROLE_STUDENT")
                .build();
        when(userDetailsService.loadUserByUsername("student@example.com"))
                .thenReturn(userDetails);
        when(refreshTokenService.rotateRefreshToken("old-refresh"))
                .thenReturn("rotated-refresh");
        when(jwtService.generateToken(userDetails)).thenReturn("new-access-token");
        when(authService.getUserResponse("student@example.com"))
                .thenReturn(new AuthResponse(
                        13L,
                        "Student",
                        "student@example.com",
                        "STUDENT",
                        "+1 415 555 0134",
                        null));

        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .cookie(new jakarta.servlet.http.Cookie(
                                "refresh_token", "old-refresh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.phoneNumber").value("+1 415 555 0134"))
                .andExpect(result -> {
                    String setCookie = result.getResponse()
                            .getHeader(HttpHeaders.SET_COOKIE);
                    assertTrue(setCookie.startsWith(
                            "refresh_token=rotated-refresh"));
                    assertTrue(setCookie.contains("Path=/api/auth"));
                    assertTrue(setCookie.contains("HttpOnly"));
                    assertTrue(setCookie.contains("SameSite=Lax"));
                    assertFalse(setCookie.contains("; Secure"));
                });

        verify(refreshTokenService).rotateRefreshToken("old-refresh");
    }

    @Test
    void unauthenticatedApiRequestReturnsJson401RatherThanOAuthRedirect()
            throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value(
                        "Authentication is required"))
                .andExpect(result -> assertEquals(
                        null,
                        result.getResponse().getHeader("Location")));
    }

    @Test
    void csrfEndpointStillIssuesToken() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void logoutRevokesRefreshCookieAndClearsIt() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .cookie(new jakarta.servlet.http.Cookie(
                                "refresh_token", "logout-refresh")))
                .andExpect(status().isNoContent())
                .andExpect(result -> {
                    String setCookie = result.getResponse()
                            .getHeader(HttpHeaders.SET_COOKIE);
                    assertTrue(setCookie.contains("refresh_token="));
                    assertTrue(setCookie.contains("Max-Age=0"));
                    assertTrue(setCookie.contains("Path=/api/auth"));
                });

        verify(refreshTokenService).revokeToken("logout-refresh");
    }
}
