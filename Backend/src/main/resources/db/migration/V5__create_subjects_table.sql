
CREATE TABLE subjects (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    admin_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    CONSTRAINT uk_subjects_admin_name
        UNIQUE (admin_id, name),

    CONSTRAINT fk_subjects_admin
        FOREIGN KEY (admin_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_subjects_admin_id
    ON subjects(admin_id);
