package com.examplatform.subject.dto;

import com.examplatform.subject.entity.Subject;
import java.time.LocalDateTime;

public record SubjectResponse(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt
) {
    public static SubjectResponse from(Subject subject) {
        return new SubjectResponse(
                subject.getId(),
                subject.getName(),
                subject.getDescription(),
                subject.getCreatedAt()
        );
    }
}