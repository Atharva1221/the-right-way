CREATE TABLE exam_results (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    score DECIMAL(10,2) NOT NULL,
    maximum_marks DECIMAL(10,2) NOT NULL,
    passed BOOLEAN NOT NULL,
    submitted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT fk_exam_results_student
        FOREIGN KEY (student_id) REFERENCES users(id),
    CONSTRAINT fk_exam_results_exam
        FOREIGN KEY (exam_id) REFERENCES exams(id),
    CONSTRAINT chk_exam_results_maximum_marks
        CHECK (maximum_marks >= 0)
);

CREATE INDEX idx_exam_results_student_id ON exam_results(student_id);
CREATE INDEX idx_exam_results_exam_id ON exam_results(exam_id);
