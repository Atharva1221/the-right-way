package com.examplatform.student.dto;

import java.time.LocalDateTime;
import java.util.List;

public record StudentExamResponse(
        Long id,
        String title,
        String description,
        String subjectName,
        Integer durationMinutes,
        LocalDateTime startAt,
        LocalDateTime endAt,
        List<StudentQuestionResponse> questions) {
}
