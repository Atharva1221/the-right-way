package com.examplatform.exam.dto;

import com.examplatform.exam.entity.Exam;
import com.examplatform.exam.entity.ExamStatus;
import com.examplatform.exam.entity.ExamVisibility;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExamResponse(
        Long id,
        String title,
        String description,
        Long subjectId,
        String subjectName,
        Integer durationMinutes,
        BigDecimal passingMarks,
        boolean shuffleQuestions,
        ExamVisibility visibility,
        ExamStatus status,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ExamResponse from(Exam exam) {
        return new ExamResponse(
                exam.getId(),
                exam.getTitle(),
                exam.getDescription(),
                exam.getSubject().getId(),
                exam.getSubject().getName(),
                exam.getDurationMinutes(),
                exam.getPassingMarks(),
                exam.isShuffleQuestions(),
                exam.getVisibility(),
                exam.getStatus(),
                exam.getStartAt(),
                exam.getEndAt(),
                exam.getCreatedAt(),
                exam.getUpdatedAt()
        );
    }
}