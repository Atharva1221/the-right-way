package com.examplatform.auth.dto;

public class TokenResponse {

    private final Long id;
    private final String name;
    private final String email;
    private final String role;
    private final String accessToken;

    public TokenResponse(
            Long id,
            String name,
            String email,
            String role,
            String accessToken) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.accessToken = accessToken;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getAccessToken() {
        return accessToken;
    }
}