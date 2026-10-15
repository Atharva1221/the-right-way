package com.examplatform.auth.controller;

import com.examplatform.auth.dto.AuthResponse;
import com.examplatform.auth.dto.LoginRequest;
import com.examplatform.auth.dto.RegisterRequest;
import com.examplatform.auth.dto.UpdatePhoneNumberRequest;
import com.examplatform.auth.service.AuthService;
import com.examplatform.auth.entity.RefreshToken;
import com.examplatform.auth.service.RefreshTokenCookieFactory;
import com.examplatform.auth.service.RefreshTokenService;
import com.examplatform.security.JwtService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;


import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public AuthController(AuthService authService,
                        RefreshTokenService refreshTokenService,
                        RefreshTokenCookieFactory refreshTokenCookieFactory,
                        JwtService jwtService,
                        UserDetailsService userDetailsService) {

            this.authService = authService;
            this.refreshTokenService = refreshTokenService;
            this.refreshTokenCookieFactory = refreshTokenCookieFactory;
            this.jwtService = jwtService;
            this.userDetailsService = userDetailsService;
        }
    

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerStudent(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthService.LoginResult result = authService.login(request);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookieFactory.create(result.refreshToken()).toString())
                .body(result.user());
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
        @CookieValue(name = "refresh_token", required = false)
        String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            return unauthorizedAndClearCookie(
                    "Refresh token is missing, invalid, expired, or revoked");
        }

        try {
            RefreshToken current =
                refreshTokenService.validateRefreshToken(rawToken);

            UserDetails userDetails =
                userDetailsService.loadUserByUsername(
                        current.getUser().getEmail());

            String newRefreshToken =
                refreshTokenService.rotateRefreshToken(rawToken);

            String accessToken =
                jwtService.generateToken(userDetails);

            AuthResponse user = authService.getUserResponse(
                userDetails.getUsername());

            AuthResponse response = new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getPhoneNumber(),
                accessToken
            );

            return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookieFactory.create(newRefreshToken).toString())
                .body(response);

        } catch (IllegalArgumentException ex) {
            return unauthorizedAndClearCookie(
                    "Refresh token is missing, invalid, expired, or revoked");
        }
    }

    private ResponseEntity<Map<String, String>> unauthorizedAndClearCookie(
            String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookieFactory.clear().toString())
                .body(Map.of("error", message));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @CookieValue(name = "refresh_token", required = false)
        String rawToken) {


            if (rawToken != null && !rawToken.isBlank()) {
                refreshTokenService.revokeToken(rawToken);
            }

        return ResponseEntity.noContent()
            .header(
                    HttpHeaders.SET_COOKIE,
                    refreshTokenCookieFactory.clear().toString())
            .build();
    }

    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(
            CsrfToken csrfToken) {

        return ResponseEntity.ok(Map.of("token", csrfToken.getToken()));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return ResponseEntity.ok(
                authService.getUserResponse(authentication.getName())
        );
    }

    @PutMapping("/me/phone-number")
    public ResponseEntity<AuthResponse> updatePhoneNumber(
            @Valid @RequestBody UpdatePhoneNumberRequest request) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return ResponseEntity.ok(authService.updatePhoneNumber(
                authentication.getName(),
                request.getPhoneNumber()));
    }

}