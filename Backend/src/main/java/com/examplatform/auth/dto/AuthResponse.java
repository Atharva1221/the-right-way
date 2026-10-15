package com.examplatform.auth.dto;

public class AuthResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
    private String phoneNumber;
    private String accessToken;

    public AuthResponse(
            Long id,
            String name,
            String email,
            String role) {
        this(id, name, email, role, null, null);
    }

    public AuthResponse(
            Long id,
            String name,
            String email,
            String role,
            String accessToken) {
        this(id, name, email, role, null, accessToken);
    }

    public AuthResponse(
            Long id,
            String name,
            String email,
            String role,
            String phoneNumber,
            String accessToken) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.phoneNumber = phoneNumber;
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public boolean isProfileComplete() {
        return "STUDENT".equals(role)
                && phoneNumber != null
                && !phoneNumber.isBlank();
    }

    public String getAccessToken() {
        return accessToken;
    }
}