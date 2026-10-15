CREATE TABLE exams (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(1000) NULL,
    subject_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL,
    duration_minutes INT NOT NULL,
    passing_marks DECIMAL(10,2) NULL,
    shuffle_questions BOOLEAN NOT NULL DEFAULT FALSE,
    max_attempts INT NOT NULL DEFAULT 1,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    start_at DATETIME(6) NULL,
    end_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    CONSTRAINT fk_exams_subject_admin
        FOREIGN KEY (subject_id, admin_id)
        REFERENCES subjects(id, admin_id),

    CONSTRAINT chk_exams_duration
        CHECK (duration_minutes > 0),

    CONSTRAINT chk_exams_passing_marks
        CHECK (passing_marks IS NULL OR passing_marks >= 0),

    CONSTRAINT chk_exams_max_attempts
        CHECK (max_attempts > 0),

    CONSTRAINT chk_exams_visibility
        CHECK (visibility IN ('PRIVATE', 'PUBLIC_LINK')),

    CONSTRAINT chk_exams_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),

    CONSTRAINT chk_exams_dates
        CHECK (
            start_at IS NULL
            OR end_at IS NULL
            OR end_at > start_at
        )
);

CREATE INDEX idx_exams_subject_id ON exams(subject_id);
CREATE INDEX idx_exams_admin_id ON exams(admin_id);