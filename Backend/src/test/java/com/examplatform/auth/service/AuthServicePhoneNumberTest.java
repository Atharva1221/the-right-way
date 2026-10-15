package com.examplatform.auth.service;

import com.examplatform.auth.dto.AuthResponse;
import com.examplatform.auth.dto.RegisterRequest;
import com.examplatform.user.enums.UserRole;
import com.examplatform.security.JwtService;
import com.examplatform.user.entity.User;
import com.examplatform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServicePhoneNumberTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                authenticationManager,
                jwtService,
                refreshTokenService);
    }

    @Test
    void registrationPersistsNormalizedPhoneAndAlwaysAssignsStudentRole() {
        RegisterRequest request = new RegisterRequest();
        request.setName(" Student ");
        request.setEmail(" STUDENT@example.com ");
        request.setPassword("long-password");
        request.setPhoneNumber("  +1 (415) 555-0134  ");
        when(userRepository.existsByEmail("student@example.com")).thenReturn(false);
        when(passwordEncoder.encode("long-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.registerStudent(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(UserRole.STUDENT, savedUser.getRole());
        assertEquals("+1 (415) 555-0134", savedUser.getPhoneNumber());
        assertEquals("encoded-password", savedUser.getPasswordHash());
        assertEquals(savedUser.getPhoneNumber(), response.getPhoneNumber());
        assertTrue(response.isProfileComplete());
    }

    @Test
    void registrationRejectsAnAlreadyRegisteredEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Student");
        request.setEmail(" STUDENT@example.com ");
        request.setPassword("long-password");
        request.setPhoneNumber("+1 (415) 555-0134");
        when(userRepository.existsByEmail("student@example.com")).thenReturn(true);

        IllegalArgumentException error = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> authService.registerStudent(request));

        assertEquals("Email is already registered", error.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void googleStudentStartsWithIncompleteProfileAndCanCompleteOwnPhone() {
        when(userRepository.findByGoogleSubject("google-subject"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmail("google@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any())).thenReturn("refresh-token");

        AuthService.LoginResult googleLogin = authService.loginWithGoogle(
                "google-subject",
                "google@example.com",
                "Google Student",
                true);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User googleUser = userCaptor.getValue();
        assertEquals(UserRole.STUDENT, googleUser.getRole());
        assertNull(googleUser.getPhoneNumber());
        assertNull(googleUser.getPasswordHash());
        assertFalse(googleLogin.user().isProfileComplete());
        assertNull(googleLogin.user().getPhoneNumber());

        when(userRepository.findByEmail("google@example.com"))
                .thenReturn(Optional.of(googleUser));
        AuthResponse completed = authService.updatePhoneNumber(
                " GOOGLE@example.com ",
                "  +61 2 5550 1234  ");

        assertEquals("+61 2 5550 1234", completed.getPhoneNumber());
        assertTrue(completed.isProfileComplete());
        verify(refreshTokenService).createRefreshToken(googleUser);
    }

    @Test
    void currentUserResponseExplicitlyMarksMissingPhoneAsIncomplete() {
        User user = new User(
                "Google Student",
                "google@example.com",
                null,
                UserRole.STUDENT,
                "google-subject");
        when(userRepository.findByEmail("google@example.com"))
                .thenReturn(Optional.of(user));

        AuthResponse response = authService.getUserResponse("google@example.com");

        assertNull(response.getPhoneNumber());
        assertFalse(response.isProfileComplete());
    }

    @Test
    void passwordLoginStillCreatesRefreshTokenAndReturnsAccessToken() {
        User user = new User(
                "Student",
                "student@example.com",
                "encoded-password",
                UserRole.STUDENT,
                null,
                "+1 415 555 0134");
        var principal = org.springframework.security.core.userdetails.User
                .withUsername("student@example.com")
                .password("encoded-password")
                .authorities("ROLE_STUDENT")
                .build();
        when(authenticationManager.authenticate(any()))
                .thenReturn(UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, principal.getAuthorities()));
        when(userRepository.findByEmail("student@example.com"))
                .thenReturn(Optional.of(user));
        when(jwtService.generateToken(principal)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user))
                .thenReturn("refresh-token");
        com.examplatform.auth.dto.LoginRequest request =
                new com.examplatform.auth.dto.LoginRequest();
        request.setEmail("student@example.com");
        request.setPassword("password");

        AuthService.LoginResult result = authService.login(request);

        assertEquals("access-token", result.user().getAccessToken());
        assertEquals("+1 415 555 0134", result.user().getPhoneNumber());
        assertEquals("refresh-token", result.refreshToken());
        verify(refreshTokenService).createRefreshToken(user);
        verifyNoMoreInteractions(refreshTokenService);
    }
}
