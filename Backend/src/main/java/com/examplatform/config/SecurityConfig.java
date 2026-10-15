package com.examplatform.config;

import com.examplatform.auth.service.GoogleAuthenticationSuccessHandler;
import com.examplatform.auth.service.GoogleOidcUserService;
import com.examplatform.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.util.StringUtils;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final GoogleOidcUserService googleOidcUserService;
    private final GoogleAuthenticationSuccessHandler googleAuthenticationSuccessHandler;
    private final String frontendUrl;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            GoogleOidcUserService googleOidcUserService,
            GoogleAuthenticationSuccessHandler googleAuthenticationSuccessHandler,
            @Value("${auth.frontend-url:http://localhost:5173}") String frontendUrl,
            Environment environment) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.googleOidcUserService = googleOidcUserService;
        this.googleAuthenticationSuccessHandler = googleAuthenticationSuccessHandler;
        this.frontendUrl = normalizeFrontendUrl(
                frontendUrl,
                environment.acceptsProfiles(Profiles.of("prod")));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origins:}") String configuredOrigins,
            Environment environment) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(parseAllowedOrigins(
                configuredOrigins,
                environment.acceptsProfiles(Profiles.of("prod"))));
        configuration.setAllowedMethods(
                List.of("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(
                List.of(
                        HttpHeaders.AUTHORIZATION,
                        HttpHeaders.CONTENT_TYPE,
                        "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository(
            @Value("${auth.refresh-cookie.secure:false}") boolean secure) {
        CookieCsrfTokenRepository repository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax"));
        return repository;
    }

    static List<String> parseAllowedOrigins(String configuredOrigins) {
        return parseAllowedOrigins(configuredOrigins, false);
    }

    static List<String> parseAllowedOrigins(
            String configuredOrigins,
            boolean production) {
        if (!StringUtils.hasText(configuredOrigins)) {
            return List.of();
        }
        List<String> origins = Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(SecurityConfig::validateOrigin)
                .toList();
        if (production && origins.stream().anyMatch(origin ->
                "http".equalsIgnoreCase(URI.create(origin).getScheme()))) {
            throw new IllegalArgumentException(
                    "Production CORS origins must use HTTPS");
        }
        return origins;
    }

    static String normalizeFrontendUrl(
            String configuredUrl,
            boolean production) {
        URI uri;
        try {
            uri = URI.create(configuredUrl);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Frontend URL must be an absolute HTTP or HTTPS origin",
                    exception);
        }
        boolean supportedScheme = "http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme());
        if (!supportedScheme
                || !StringUtils.hasText(uri.getHost())
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null
                || (StringUtils.hasText(uri.getPath())
                        && !"/".equals(uri.getPath()))) {
            throw new IllegalArgumentException(
                    "Frontend URL must contain only an HTTP or HTTPS origin");
        }
        if (production && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException(
                    "Production frontend URL must use HTTPS");
        }
        return configuredUrl.replaceAll("/+$", "");
    }

    private static String validateOrigin(String origin) {
        if (origin.contains("*")) {
            throw new IllegalArgumentException(
                    "CORS origins must be explicit; wildcard origins are not allowed");
        }
        URI uri;
        try {
            uri = URI.create(origin);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "CORS origins must be valid HTTP or HTTPS origins",
                    exception);
        }
        if (!("http".equalsIgnoreCase(uri.getScheme())
                        || "https".equalsIgnoreCase(uri.getScheme()))
                || !StringUtils.hasText(uri.getHost())
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null
                || (StringUtils.hasText(uri.getPath())
                        && !"/".equals(uri.getPath()))) {
            throw new IllegalArgumentException(
                    "CORS origins must contain only an HTTP or HTTPS origin");
        }
        String normalized = origin.endsWith("/")
                ? origin.substring(0, origin.length() - 1)
                : origin;
        return normalized;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            CookieCsrfTokenRepository csrfTokenRepository)
            throws Exception {

        AuthenticationEntryPoint apiAuthenticationEntryPoint =
                (request, response, exception) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write(
                            "{\"error\":\"Authentication is required\"}");
                };
        RequestMatcher apiRequestMatcher =
                request -> request.getServletPath().startsWith("/api/");

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
            )
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                        apiAuthenticationEntryPoint,
                        apiRequestMatcher))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/health").permitAll()
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/register",
                    "/api/auth/login"
                ).permitAll()
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/auth/csrf",
                    "/oauth2/authorization/**",
                    "/login/oauth2/code/**"
                ).permitAll()
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/refresh",
                    "/api/auth/logout"
                ).permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/student/**").hasRole("STUDENT")
                .requestMatchers(HttpMethod.PUT, "/api/auth/me/phone-number")
                    .hasRole("STUDENT")
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(googleOidcUserService))
                .successHandler(googleAuthenticationSuccessHandler)
                .failureHandler((request, response, exception) -> {
                    response.sendRedirect(frontendUrl + "/auth/callback?oauth=error");
                })
            )
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    private static final class SpaCsrfTokenRequestHandler
            implements CsrfTokenRequestHandler {

        private final CsrfTokenRequestHandler xor =
                new XorCsrfTokenRequestAttributeHandler();
        private final CsrfTokenRequestHandler plain =
                new CsrfTokenRequestAttributeHandler();
        @Override
        public void handle(
                HttpServletRequest request,
                HttpServletResponse response,
                Supplier<CsrfToken> csrfToken) {
            xor.handle(request, response, csrfToken);
            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(
                HttpServletRequest request,
                CsrfToken csrfToken) {
            String headerValue = request.getHeader(csrfToken.getHeaderName());
            if (StringUtils.hasText(headerValue)
                    && csrfToken.getToken().equals(headerValue)) {
                return plain.resolveCsrfTokenValue(request, csrfToken);
            }
            return xor.resolveCsrfTokenValue(request, csrfToken);
        }
    }
}