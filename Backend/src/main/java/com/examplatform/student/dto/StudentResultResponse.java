package com.examplatform.student.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StudentResultResponse(
        Long resultId,
        Long examId,
        String examTitle,
        String subjectName,
        BigDecimal score,
        BigDecimal maximumMarks,
        boolean passed,
        LocalDateTime submittedAt) {
}
