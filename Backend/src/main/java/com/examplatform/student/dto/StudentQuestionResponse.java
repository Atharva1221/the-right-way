package com.examplatform.student.dto;

import java.math.BigDecimal;
import java.util.List;

public record StudentQuestionResponse(
        Long id,
        String questionText,
        Integer questionOrder,
        BigDecimal marks,
        BigDecimal negativeMarks,
        List<StudentOptionResponse> options) {
}
