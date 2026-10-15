package com.examplatform.student.dto;

import java.math.BigDecimal;

public record StudentSubjectPerformance(
        Long subjectId,
        String subjectName,
        long testsTaken,
        long testsPassed,
        long testsFailed,
        BigDecimal averageScorePercentage) {
}
