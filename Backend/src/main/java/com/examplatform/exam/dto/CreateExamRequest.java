package com.examplatform.exam.dto;

import com.examplatform.exam.entity.ExamVisibility;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateExamRequest(
        @NotBlank
        @Size(max = 150)
        String title,

        @Size(max = 1000)
        String description,

        @NotNull
        @Positive
        Long subjectId,

        @NotNull
        @Positive
        Integer durationMinutes,

        @DecimalMin("0.00")
        BigDecimal passingMarks,

        boolean shuffleQuestions,

        @NotNull
        ExamVisibility visibility,

        LocalDateTime startAt,

        LocalDateTime endAt
) {
}