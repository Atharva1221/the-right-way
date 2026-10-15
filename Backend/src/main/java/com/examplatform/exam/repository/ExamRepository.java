package com.examplatform.exam.repository;

import com.examplatform.exam.entity.Exam;
import com.examplatform.exam.entity.ExamStatus;
import com.examplatform.exam.entity.ExamVisibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findAllByAdminIdOrderByCreatedAtDesc(Long adminId);

    Optional<Exam> findByIdAndAdminId(Long id, Long adminId);

    List<Exam> findAllByStatusAndVisibilityOrderByCreatedAtDesc(
            ExamStatus status,
            ExamVisibility visibility);

    Optional<Exam> findByIdAndStatusAndVisibility(
            Long id,
            ExamStatus status,
            ExamVisibility visibility);

    List<Exam> findAllBySubjectIdAndAdminIdOrderByCreatedAtDesc(
            Long subjectId,
            Long adminId
    );

    boolean existsBySubjectIdAndAdminId(Long subjectId, Long adminId);
}