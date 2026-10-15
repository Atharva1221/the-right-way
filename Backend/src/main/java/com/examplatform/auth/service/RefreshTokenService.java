package com.examplatform.auth.service;

import com.examplatform.auth.entity.RefreshToken;
import com.examplatform.auth.repository.RefreshTokenRepository;
import com.examplatform.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long expirationMillis;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${jwt.refresh-expiration-ms:10800000}")
            long expirationMillis) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.expirationMillis = expirationMillis;
    }

    @Transactional
    public String createRefreshToken(User user) {
        String rawToken = generateRawToken();

        refreshTokenRepository.save(new RefreshToken(
                user,
                hashToken(rawToken),
                Instant.now().plusMillis(expirationMillis)
        ));

        return rawToken;
    }

    @Transactional
    public RefreshToken validateRefreshToken(String rawToken) {
        RefreshToken token = refreshTokenRepository
                .findByTokenHash(hashToken(rawToken))
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid refresh token"));

        validateActive(token);
        return token;
    }

    @Transactional
    public String rotateRefreshToken(String rawToken) {
        RefreshToken current = refreshTokenRepository
                .findByTokenHashForUpdate(hashToken(rawToken))
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid refresh token"));

        validateActive(current);

        current.revoke();
        refreshTokenRepository.flush();

        return createRefreshToken(current.getUser());
    }

    @Transactional
    public void revokeToken(String rawToken) {
        refreshTokenRepository.findByTokenHash(hashToken(rawToken))
                .ifPresent(RefreshToken::revoke);
    }

    private void validateActive(RefreshToken token) {
        if (token.isRevoked()
                || !token.getExpiresAt().isAfter(Instant.now())) {
            throw new IllegalArgumentException(
                    "Invalid or expired refresh token");
        }
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));

            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to hash refresh token", exception);
        }
    }
}