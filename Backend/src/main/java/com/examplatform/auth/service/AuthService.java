package com.examplatform.auth.service;

import com.examplatform.auth.dto.AuthResponse;
import com.examplatform.auth.dto.LoginRequest;
import com.examplatform.auth.dto.RegisterRequest;
import com.examplatform.user.enums.UserRole;
import com.examplatform.security.JwtService;
import com.examplatform.user.entity.User;
import com.examplatform.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResponse registerStudent(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = new User(
                request.getName().trim(),
                email,
                passwordEncoder.encode(request.getPassword()),
                UserRole.STUDENT,
                null,
                request.getPhoneNumber().trim()
        );

        User savedUser = userRepository.save(user);

        return new AuthResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                savedUser.getPhoneNumber(),
                null
        );
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());

        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid email or password"));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        AuthResponse userResponse = new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getPhoneNumber(),
                accessToken
        );

        return new LoginResult(userResponse, refreshToken);
    }

    @Transactional
    public LoginResult loginWithGoogle(
            String googleSubject,
            String email,
            String name,
            boolean emailVerified) {
        if (!emailVerified) {
            throw new IllegalArgumentException(
                    "Google account must have a verified email address");
        }
        if (!StringUtils.hasText(googleSubject) || !StringUtils.hasText(email)) {
            throw new IllegalArgumentException(
                    "Google did not provide a valid identity");
        }

        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByGoogleSubject(googleSubject)
                .orElseGet(() -> {
                    if (userRepository.existsByEmail(normalizedEmail)) {
                        throw new IllegalArgumentException(
                                "An account with this email already exists. "
                                        + "Sign in with that account; automatic account linking is unavailable.");
                    }

                    String displayName = StringUtils.hasText(name)
                            ? name.trim()
                            : normalizedEmail.substring(
                                    0, normalizedEmail.indexOf('@'));
                    if (displayName.length() > 100) {
                        displayName = displayName.substring(0, 100);
                    }

                    return userRepository.save(new User(
                            displayName,
                            normalizedEmail,
                            null,
                            UserRole.STUDENT,
                            googleSubject,
                            null));
                });

        org.springframework.security.core.userdetails.UserDetails userDetails =
                org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPasswordHash() == null
                                ? ""
                                : user.getPasswordHash())
                        .authorities("ROLE_" + user.getRole().name())
                        .build();

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(user);
        AuthResponse userResponse = new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getPhoneNumber(),
                accessToken);

        return new LoginResult(userResponse, refreshToken);
    }

    public AuthResponse getUserResponse(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getPhoneNumber(),
                null
        );
    }

    @Transactional
    public AuthResponse updatePhoneNumber(String email, String phoneNumber) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));
        user.updatePhoneNumber(phoneNumber);

        return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getPhoneNumber(),
                null
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record LoginResult(
            AuthResponse user,
            String refreshToken) {
    }
}