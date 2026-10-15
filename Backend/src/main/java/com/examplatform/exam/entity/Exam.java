package com.examplatform.exam.entity;

import com.examplatform.subject.entity.Subject;
import com.examplatform.user.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "exams")
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "passing_marks", precision = 10, scale = 2)
    private BigDecimal passingMarks;

    @Column(name = "shuffle_questions", nullable = false)
    private boolean shuffleQuestions = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExamVisibility visibility = ExamVisibility.PRIVATE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExamStatus status = ExamStatus.DRAFT;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Exam() {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Subject getSubject() {
        return subject;
    }

    public User getAdmin() {
        return admin;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public BigDecimal getPassingMarks() {
        return passingMarks;
    }

    public boolean isShuffleQuestions() {
        return shuffleQuestions;
    }

    public ExamVisibility getVisibility() {
        return visibility;
    }

    public ExamStatus getStatus() {
        return status;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateDetails(
            String title,
            String description,
            Subject subject,
            Integer durationMinutes,
            BigDecimal passingMarks,
            boolean shuffleQuestions,
            ExamVisibility visibility,
            LocalDateTime startAt,
            LocalDateTime endAt) {

        this.title = title.trim();
        this.description = description;
        this.subject = subject;
        this.durationMinutes = durationMinutes;
        this.passingMarks = passingMarks;
        this.shuffleQuestions = shuffleQuestions;
        this.visibility = visibility;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public void updateVisibility(ExamVisibility visibility) {
        this.visibility = visibility;
    }

    public void publish() {
        this.status = ExamStatus.PUBLISHED;
    }

    public void archive() {
        this.status = ExamStatus.ARCHIVED;
    }

    public void setInitialDetails(
            String title,
            String description,
            Subject subject,
            User admin,
            Integer durationMinutes,
            BigDecimal passingMarks,
            boolean shuffleQuestions,
            ExamVisibility visibility,
            LocalDateTime startAt,
            LocalDateTime endAt) {

        this.title = title.trim();
        this.description = description;
        this.subject = subject;
        this.admin = admin;
        this.durationMinutes = durationMinutes;
        this.passingMarks = passingMarks;
        this.shuffleQuestions = shuffleQuestions;
        this.visibility = visibility;
        this.startAt = startAt;
        this.endAt = endAt;
    }
}