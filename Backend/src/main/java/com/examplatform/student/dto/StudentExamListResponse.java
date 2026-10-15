package com.examplatform.student.dto;

import java.time.LocalDateTime;

public record StudentExamListResponse(
        Long id,
        String title,
        String description,
        String subjectName,
        Integer durationMinutes,
        LocalDateTime startAt,
        LocalDateTime endAt) {
}
