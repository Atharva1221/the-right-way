package com.examplatform.question.entity;

import com.examplatform.exam.entity.Exam;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(
        name = "exam_questions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_exam_questions_order",
                columnNames = {"exam_id", "question_order"}))
public class ExamQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal marks;

    @Column(name = "negative_marks", nullable = false, precision = 10, scale = 2)
    private BigDecimal negativeMarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionOption> options = new ArrayList<>();

    protected ExamQuestion() {
    }

    public ExamQuestion(
            Exam exam,
            String questionText,
            String explanation,
            Integer questionOrder,
            BigDecimal marks,
            BigDecimal negativeMarks) {
        this.exam = exam;
        updateDetails(questionText, explanation, questionOrder, marks, negativeMarks);
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

    public Exam getExam() {
        return exam;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getExplanation() {
        return explanation;
    }

    public Integer getQuestionOrder() {
        return questionOrder;
    }

    public BigDecimal getMarks() {
        return marks;
    }

    public BigDecimal getNegativeMarks() {
        return negativeMarks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<QuestionOption> getOptions() {
        return options.stream()
                .sorted(Comparator.comparing(
                        QuestionOption::getOptionOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public void updateDetails(
            String questionText,
            String explanation,
            Integer questionOrder,
            BigDecimal marks,
            BigDecimal negativeMarks) {
        this.questionText = questionText.trim();
        this.explanation = explanation;
        this.questionOrder = questionOrder;
        this.marks = marks;
        this.negativeMarks = negativeMarks;
    }

    public void addOption(QuestionOption option) {
        option.attachTo(this);
        options.add(option);
    }

    public void clearOptions() {
        options.clear();
    }

    public boolean hasValidContent() {
        if (questionText == null
                || questionText.isBlank()
                || questionOrder == null
                || questionOrder <= 0
                || marks == null
                || marks.signum() < 0
                || negativeMarks == null
                || negativeMarks.signum() < 0
                || options.size() < 2) {
            return false;
        }

        long correctCount = 0;
        Set<Integer> optionOrders = new HashSet<>();
        for (QuestionOption option : options) {
            if (option.getOptionText() == null
                    || option.getOptionText().isBlank()
                    || option.getOptionOrder() == null
                    || option.getOptionOrder() <= 0
                    || !optionOrders.add(option.getOptionOrder())) {
                return false;
            }
            if (option.isCorrect()) {
                correctCount++;
            }
        }
        return correctCount == 1;
    }
}
