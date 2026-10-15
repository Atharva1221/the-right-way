package com.examplatform.student.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ExamSubmissionRequest(
        @NotNull(message = "Answers are required")
        List<@NotNull(message = "Answers must not contain null entries")
                @Valid SubmittedAnswer> answers) {

    public record SubmittedAnswer(
            @NotNull(message = "Question ID is required") Long questionId,
            Long selectedOptionId) {
    }
}
