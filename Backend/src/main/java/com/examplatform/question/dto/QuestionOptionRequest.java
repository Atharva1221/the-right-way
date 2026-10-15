package com.examplatform.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record QuestionOptionRequest(
        @NotBlank(message = "Option text must not be blank")
        String optionText,

        boolean correct,

        @NotNull(message = "Option order is required")
        @Positive(message = "Option order must be positive")
        Integer optionOrder
) {
}
