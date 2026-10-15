package com.examplatform.auth.dto;

public record CurrentUserResponse(
        Long id,
        String name,
        String email,
        String role
) {
}