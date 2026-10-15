ALTER TABLE exams
    DROP CHECK chk_exams_max_attempts,
    DROP COLUMN max_attempts;
