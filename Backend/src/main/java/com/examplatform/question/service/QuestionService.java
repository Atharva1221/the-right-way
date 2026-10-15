package com.examplatform.question.service;

import com.examplatform.question.dto.CreateQuestionRequest;
import com.examplatform.question.dto.QuestionOptionRequest;
import com.examplatform.question.dto.UpdateQuestionRequest;
import com.examplatform.exam.entity.Exam;
import com.examplatform.question.entity.ExamQuestion;
import com.examplatform.exam.entity.ExamStatus;
import com.examplatform.question.entity.QuestionOption;
import com.examplatform.question.repository.ExamQuestionRepository;
import com.examplatform.exam.repository.ExamRepository;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository questionRepository;

    public QuestionService(
            ExamRepository examRepository,
            ExamQuestionRepository questionRepository) {
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional
    public ExamQuestion createQuestion(
            Long adminId,
            Long examId,
            CreateQuestionRequest request) {
        Exam exam = getOwnedExam(adminId, examId);
        ensureDraft(exam);
        validateQuestion(
                request.questionText(),
                request.questionOrder(),
                request.marks(),
                request.negativeMarks(),
                request.options());
        ensureQuestionOrderAvailable(examId, request.questionOrder(), null);

        ExamQuestion question = new ExamQuestion(
                exam,
                request.questionText(),
                request.explanation(),
                request.questionOrder(),
                request.marks(),
                request.negativeMarks());
        addOptions(question, request.options());
        return questionRepository.save(question);
    }

    @Transactional(readOnly = true)
    public List<ExamQuestion> getQuestions(Long adminId, Long examId) {
        getOwnedExam(adminId, examId);
        return questionRepository.findAllByExamIdOrderByQuestionOrderAsc(examId);
    }

    @Transactional(readOnly = true)
    public ExamQuestion getQuestion(Long adminId, Long examId, Long questionId) {
        getOwnedExam(adminId, examId);
        return questionRepository.findByIdAndExamId(questionId, examId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));
    }

    @Transactional
    public ExamQuestion updateQuestion(
            Long adminId,
            Long examId,
            Long questionId,
            UpdateQuestionRequest request) {
        Exam exam = getOwnedExam(adminId, examId);
        ensureDraft(exam);
        validateQuestion(
                request.questionText(),
                request.questionOrder(),
                request.marks(),
                request.negativeMarks(),
                request.options());

        ExamQuestion question = questionRepository
                .findByIdAndExamId(questionId, examId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));
        ensureQuestionOrderAvailable(examId, request.questionOrder(), questionId);

        question.updateDetails(
                request.questionText(),
                request.explanation(),
                request.questionOrder(),
                request.marks(),
                request.negativeMarks());
        question.clearOptions();
        questionRepository.flush();
        addOptions(question, request.options());
        return questionRepository.save(question);
    }

    @Transactional
    public void deleteQuestion(Long adminId, Long examId, Long questionId) {
        Exam exam = getOwnedExam(adminId, examId);
        ensureDraft(exam);
        ExamQuestion question = questionRepository
                .findByIdAndExamId(questionId, examId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));
        questionRepository.delete(question);
    }

    private Exam getOwnedExam(Long adminId, Long examId) {
        return examRepository.findByIdAndAdminId(examId, adminId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));
    }

    private void ensureDraft(Exam exam) {
        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw new IllegalStateException("Only draft exams can be edited");
        }
    }

    private void ensureQuestionOrderAvailable(
            Long examId,
            Integer questionOrder,
            Long currentQuestionId) {
        boolean alreadyExists = currentQuestionId == null
                ? questionRepository.existsByExamIdAndQuestionOrder(
                        examId, questionOrder)
                : questionRepository.existsByExamIdAndQuestionOrderAndIdNot(
                        examId, questionOrder, currentQuestionId);
        if (alreadyExists) {
            throw new IllegalArgumentException(
                    "Question order already exists in this exam");
        }
    }

    private void validateQuestion(
            String questionText,
            Integer questionOrder,
            BigDecimal marks,
            BigDecimal negativeMarks,
            List<QuestionOptionRequest> options) {
        if (questionText == null || questionText.isBlank()) {
            throw new IllegalArgumentException("Question text must not be blank");
        }
        if (questionOrder == null || questionOrder <= 0) {
            throw new IllegalArgumentException("Question order must be positive");
        }
        if (marks == null || marks.signum() < 0) {
            throw new IllegalArgumentException("Marks must be non-negative");
        }
        if (negativeMarks == null || negativeMarks.signum() < 0) {
            throw new IllegalArgumentException(
                    "Negative marks must be non-negative");
        }
        if (options == null || options.size() < 2) {
            throw new IllegalArgumentException("At least two options are required");
        }

        int correctCount = 0;
        Set<Integer> optionOrders = new HashSet<>();
        for (QuestionOptionRequest option : options) {
            if (option == null) {
                throw new IllegalArgumentException(
                        "Options must not contain null entries");
            }
            if (option.optionText() == null || option.optionText().isBlank()) {
                throw new IllegalArgumentException("Option text must not be blank");
            }
            if (option.optionOrder() == null || option.optionOrder() <= 0) {
                throw new IllegalArgumentException("Option order must be positive");
            }
            if (!optionOrders.add(option.optionOrder())) {
                throw new IllegalArgumentException("Option orders must be unique");
            }
            if (option.correct()) {
                correctCount++;
            }
        }
        if (correctCount != 1) {
            throw new IllegalArgumentException(
                    "Exactly one option must be marked correct");
        }
    }

    private void addOptions(
            ExamQuestion question,
            List<QuestionOptionRequest> options) {
        options.forEach(option -> question.addOption(new QuestionOption(
                option.optionText(),
                option.correct(),
                option.optionOrder())));
    }
}
