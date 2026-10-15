package com.examplatform.result.repository;

import com.examplatform.result.entity.ExamResult;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    List<ExamResult> findAllByStudentIdOrderBySubmittedAtDesc(Long studentId);

    Optional<ExamResult> findByIdAndStudent_Id(Long id, Long studentId);
}
