package com.examplatform.student.dto;

import java.math.BigDecimal;
import java.util.List;

public record StudentDashboardResponse(
        long testsTaken,
        long testsPassed,
        long testsFailed,
        BigDecimal passRate,
        List<StudentSubjectPerformance> subjectPerformance) {
}
