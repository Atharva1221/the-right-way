package com.examplatform.question.repository;

import com.examplatform.question.entity.ExamQuestion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {

    @EntityGraph(attributePaths = "options")
    List<ExamQuestion> findAllByExamIdOrderByQuestionOrderAsc(Long examId);

    @EntityGraph(attributePaths = "options")
    Optional<ExamQuestion> findByIdAndExamId(Long id, Long examId);

    long countByExamId(Long examId);

    boolean existsByExamIdAndQuestionOrder(Long examId, Integer questionOrder);

    boolean existsByExamIdAndQuestionOrderAndIdNot(
            Long examId,
            Integer questionOrder,
            Long id);
}
