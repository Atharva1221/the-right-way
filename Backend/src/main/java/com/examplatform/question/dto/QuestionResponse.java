package com.examplatform.question.dto;

import com.examplatform.question.entity.ExamQuestion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record QuestionResponse(
        Long id,
        String questionText,
        String explanation,
        Integer questionOrder,
        BigDecimal marks,
        BigDecimal negativeMarks,
        List<QuestionOptionResponse> options,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static QuestionResponse from(ExamQuestion question) {
        return new QuestionResponse(
                question.getId(),
                question.getQuestionText(),
                question.getExplanation(),
                question.getQuestionOrder(),
                question.getMarks(),
                question.getNegativeMarks(),
                question.getOptions().stream()
                        .map(QuestionOptionResponse::from)
                        .toList(),
                question.getCreatedAt(),
                question.getUpdatedAt());
    }
}
