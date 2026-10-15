package com.examplatform.subject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSubjectRequest(
        @NotBlank(message = "Subject name is required")
        @Size(max = 100, message = "Subject name cannot exceed 100 characters")
        String name,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description
) {
}