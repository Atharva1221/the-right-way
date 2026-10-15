package com.examplatform.question.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record UpdateQuestionRequest(
        @NotBlank(message = "Question text must not be blank")
        String questionText,

        String explanation,

        @NotNull(message = "Question order is required")
        @Positive(message = "Question order must be positive")
        Integer questionOrder,

        @NotNull(message = "Marks are required")
        @DecimalMin(value = "0.00", message = "Marks cannot be negative")
        BigDecimal marks,

        @NotNull(message = "Negative marks are required")
        @DecimalMin(value = "0.00", message = "Negative marks cannot be negative")
        BigDecimal negativeMarks,

        @NotNull(message = "At least two options are required")
        @Size(min = 2, message = "At least two options are required")
        List<@NotNull(message = "Options must not contain null entries")
                @Valid QuestionOptionRequest> options
) {

    @AssertTrue(message = "Exactly one option must be marked correct")
    public boolean isExactlyOneOptionCorrect() {
        return options != null
                && options.stream()
                        .filter(Objects::nonNull)
                        .filter(QuestionOptionRequest::correct)
                        .count() == 1;
    }

    @AssertTrue(message = "Option orders must be unique")
    public boolean isOptionOrdersUnique() {
        if (options == null) {
            return true;
        }
        Set<Integer> orders = new HashSet<>();
        return options.stream()
                .filter(Objects::nonNull)
                .map(QuestionOptionRequest::optionOrder)
                .filter(Objects::nonNull)
                .allMatch(orders::add);
    }
}
