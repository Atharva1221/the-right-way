package com.examplatform.student.dto;

import java.math.BigDecimal;

public record ExamSubmissionResult(
        Long resultId,
        Long examId,
        int totalQuestions,
        int correctAnswers,
        int incorrectAnswers,
        int unansweredQuestions,
        BigDecimal score,
        BigDecimal passingMarks,
        Boolean passed) {
}
