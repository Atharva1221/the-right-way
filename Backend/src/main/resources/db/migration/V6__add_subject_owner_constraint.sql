ALTER TABLE subjects
ADD CONSTRAINT uk_subjects_id_admin
UNIQUE (id, admin_id);