CREATE TABLE exam_questions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    explanation TEXT NULL,
    question_order INT NOT NULL,
    marks DECIMAL(10,2) NOT NULL DEFAULT 1.00,
    negative_marks DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_exam_questions_exam
        FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
    CONSTRAINT chk_exam_questions_order CHECK (question_order > 0),
    CONSTRAINT chk_exam_questions_marks CHECK (marks >= 0),
    CONSTRAINT chk_exam_questions_negative_marks CHECK (negative_marks >= 0),
    CONSTRAINT uk_exam_questions_order UNIQUE (exam_id, question_order)
);

CREATE TABLE question_options (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    option_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    option_order INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_question_options_question
        FOREIGN KEY (question_id) REFERENCES exam_questions(id) ON DELETE CASCADE,
    CONSTRAINT chk_question_options_order CHECK (option_order > 0),
    CONSTRAINT uk_question_options_order UNIQUE (question_id, option_order)
);

CREATE INDEX idx_exam_questions_exam_id ON exam_questions(exam_id);
CREATE INDEX idx_question_options_question_id ON question_options(question_id);