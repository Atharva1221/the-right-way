package com.examplatform.result.entity;

import com.examplatform.exam.entity.Exam;
import com.examplatform.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_results")
public class ExamResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal score;

    @Column(name = "maximum_marks", nullable = false, precision = 10, scale = 2)
    private BigDecimal maximumMarks;

    @Column(nullable = false)
    private boolean passed;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    protected ExamResult() {
    }

    public ExamResult(
            User student,
            Exam exam,
            BigDecimal score,
            BigDecimal maximumMarks,
            boolean passed) {
        this.student = student;
        this.exam = exam;
        this.score = score;
        this.maximumMarks = maximumMarks;
        this.passed = passed;
    }

    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public User getStudent() {
        return student;
    }

    public Exam getExam() {
        return exam;
    }

    public BigDecimal getScore() {
        return score;
    }

    public BigDecimal getMaximumMarks() {
        return maximumMarks;
    }

    public boolean isPassed() {
        return passed;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
}
