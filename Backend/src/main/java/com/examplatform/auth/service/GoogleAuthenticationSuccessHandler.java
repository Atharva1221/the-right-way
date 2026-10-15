package com.examplatform.auth.service;

import com.examplatform.auth.service.AuthService.LoginResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class GoogleAuthenticationSuccessHandler
        implements AuthenticationSuccessHandler {

    private final ObjectProvider<AuthService> authServiceProvider;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;
    private final String frontendUrl;

    public GoogleAuthenticationSuccessHandler(
            ObjectProvider<AuthService> authServiceProvider,
            RefreshTokenService refreshTokenService,
            RefreshTokenCookieFactory refreshTokenCookieFactory,
            @Value("${auth.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.authServiceProvider = authServiceProvider;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenCookieFactory = refreshTokenCookieFactory;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OidcUser googleUser)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        LoginResult result = null;
        try {
            result = authServiceProvider.getObject().loginWithGoogle(
                    googleUser.getSubject(),
                    googleUser.getClaimAsString("email"),
                    googleUser.getClaimAsString("name"),
                    Boolean.TRUE.equals(
                            googleUser.getClaimAsBoolean("email_verified")));

            response.addHeader(
                    HttpHeaders.SET_COOKIE,
                    refreshTokenCookieFactory.create(result.refreshToken()).toString());
            response.sendRedirect(callbackUrl("success", null));
        } catch (IllegalArgumentException exception) {
            String reason = exception.getMessage() != null
                    && exception.getMessage().startsWith("An account with this email")
                    ? "account-conflict"
                    : null;
            response.sendRedirect(callbackUrl("error", reason));
        } catch (IOException | RuntimeException exception) {
            if (result != null) {
                refreshTokenService.revokeToken(result.refreshToken());
            }
            throw exception;
        }
    }

    private String callbackUrl(String status, String reason) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(frontendUrl)
                .path("/auth/callback")
                .queryParam("oauth", status);
        if (reason != null) {
            builder.queryParam("error", reason);
        }
        return builder.build().encode().toUriString();
    }
}
