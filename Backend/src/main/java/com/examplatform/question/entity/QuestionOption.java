package com.examplatform.question.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "question_options",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_question_options_order",
                columnNames = {"question_id", "option_order"}))
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private ExamQuestion question;

    @Column(name = "option_text", nullable = false, columnDefinition = "TEXT")
    private String optionText;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "option_order", nullable = false)
    private Integer optionOrder;

    protected QuestionOption() {
    }

    public QuestionOption(String optionText, boolean correct, Integer optionOrder) {
        this.optionText = optionText.trim();
        this.correct = correct;
        this.optionOrder = optionOrder;
    }

    public Long getId() {
        return id;
    }

    public ExamQuestion getQuestion() {
        return question;
    }

    public String getOptionText() {
        return optionText;
    }

    public boolean isCorrect() {
        return correct;
    }

    public Integer getOptionOrder() {
        return optionOrder;
    }

    void attachTo(ExamQuestion question) {
        this.question = question;
    }
}
