package com.examplatform.question.repository;

import com.examplatform.question.entity.QuestionOption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {

    List<QuestionOption> findAllByQuestionIdOrderByOptionOrderAsc(Long questionId);
}
