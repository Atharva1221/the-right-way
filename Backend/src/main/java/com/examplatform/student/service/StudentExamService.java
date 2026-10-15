package com.examplatform.student.service;

import com.examplatform.student.dto.ExamSubmissionRequest;
import com.examplatform.student.dto.ExamSubmissionResult;
import com.examplatform.student.dto.StudentExamListResponse;
import com.examplatform.student.dto.StudentExamResponse;
import com.examplatform.student.dto.StudentOptionResponse;
import com.examplatform.student.dto.StudentQuestionResponse;
import com.examplatform.student.dto.StudentResultResponse;
import com.examplatform.exam.entity.Exam;
import com.examplatform.question.entity.ExamQuestion;
import com.examplatform.exam.entity.ExamStatus;
import com.examplatform.exam.entity.ExamVisibility;
import com.examplatform.question.entity.QuestionOption;
import com.examplatform.result.entity.ExamResult;
import com.examplatform.question.repository.ExamQuestionRepository;
import com.examplatform.exam.repository.ExamRepository;
import com.examplatform.result.repository.ExamResultRepository;
import com.examplatform.user.entity.User;
import com.examplatform.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StudentExamService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository questionRepository;
    private final ExamResultRepository resultRepository;
    private final UserRepository userRepository;

    public StudentExamService(
            ExamRepository examRepository,
            ExamQuestionRepository questionRepository,
            ExamResultRepository resultRepository,
            UserRepository userRepository) {
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.resultRepository = resultRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<StudentExamListResponse> listAvailableExams() {
        LocalDateTime now = LocalDateTime.now();
        return examRepository
                .findAllByStatusAndVisibilityOrderByCreatedAtDesc(
                        ExamStatus.PUBLISHED,
                        ExamVisibility.PUBLIC_LINK)
                .stream()
                .filter(exam -> isWithinAvailabilityWindow(exam, now))
                .map(this::toListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentExamResponse getExam(Long examId) {
        Exam exam = getAccessibleExam(examId);
        List<StudentQuestionResponse> questions = questionRepository
                .findAllByExamIdOrderByQuestionOrderAsc(examId)
                .stream()
                .map(this::toStudentQuestion)
                .toList();

        return new StudentExamResponse(
                exam.getId(),
                exam.getTitle(),
                exam.getDescription(),
                exam.getSubject().getName(),
                exam.getDurationMinutes(),
                exam.getStartAt(),
                exam.getEndAt(),
                questions);
    }

    @Transactional
    public ExamSubmissionResult submitAnswers(
            Long examId,
            ExamSubmissionRequest request) {
        Exam exam = getAccessibleExam(examId);
        List<ExamQuestion> questions =
                questionRepository.findAllByExamIdOrderByQuestionOrderAsc(examId);
        Map<Long, ExamQuestion> questionsById = new HashMap<>();
        for (ExamQuestion question : questions) {
            questionsById.put(question.getId(), question);
        }

        Map<Long, Long> selectedOptionsByQuestion = new HashMap<>();
        Set<Long> submittedQuestionIds = new HashSet<>();
        for (ExamSubmissionRequest.SubmittedAnswer answer : request.answers()) {
            if (answer == null || answer.questionId() == null) {
                throw new IllegalArgumentException(
                        "Every answer must include a question ID");
            }
            if (!submittedQuestionIds.add(answer.questionId())) {
                throw new IllegalArgumentException(
                        "A question may only be submitted once");
            }
            if (!questionsById.containsKey(answer.questionId())) {
                throw new IllegalArgumentException(
                        "A submitted question does not belong to this exam");
            }
            selectedOptionsByQuestion.put(
                    answer.questionId(),
                    answer.selectedOptionId());
        }

        int correctAnswers = 0;
        int incorrectAnswers = 0;
        int unansweredQuestions = 0;
        BigDecimal score = BigDecimal.ZERO;

        for (ExamQuestion question : questions) {
            Long selectedOptionId =
                    selectedOptionsByQuestion.get(question.getId());
            if (selectedOptionId == null) {
                unansweredQuestions++;
                continue;
            }

            QuestionOption selectedOption = question.getOptions().stream()
                    .filter(option -> selectedOptionId.equals(option.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "A selected option does not belong to its question"));

            if (selectedOption.isCorrect()) {
                correctAnswers++;
                score = score.add(question.getMarks());
            } else {
                incorrectAnswers++;
                score = score.subtract(question.getNegativeMarks());
            }
        }

        BigDecimal passingMarks = exam.getPassingMarks();
        Boolean passed = passingMarks == null
                ? null
                : score.compareTo(passingMarks) >= 0;

        BigDecimal maximumMarks = questions.stream()
                .map(ExamQuestion::getMarks)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required");
        }
        User student = getAuthenticatedStudent(authentication);
        ExamResult savedResult = resultRepository.save(new ExamResult(
                student,
                exam,
                score,
                maximumMarks,
                Boolean.TRUE.equals(passed)));

        return new ExamSubmissionResult(
                savedResult.getId(),
                examId,
                questions.size(),
                correctAnswers,
                incorrectAnswers,
                unansweredQuestions,
                score,
                passingMarks,
                passed);
    }

    @Transactional(readOnly = true)
    public StudentResultResponse getResult(Long resultId) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required");
        }
        User student = getAuthenticatedStudent(authentication);
        ExamResult result = resultRepository
                .findByIdAndStudent_Id(resultId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Result not found"));

        return new StudentResultResponse(
                result.getId(),
                result.getExam().getId(),
                result.getExam().getTitle(),
                result.getExam().getSubject().getName(),
                result.getScore(),
                result.getMaximumMarks(),
                result.isPassed(),
                result.getSubmittedAt());
    }

    private User getAuthenticatedStudent(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated student was not found"));
    }

    private Exam getAccessibleExam(Long examId) {
        Exam exam = examRepository.findByIdAndStatusAndVisibility(
                        examId,
                        ExamStatus.PUBLISHED,
                        ExamVisibility.PUBLIC_LINK)
                .filter(candidate ->
                        isWithinAvailabilityWindow(candidate, LocalDateTime.now()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Exam not found"));
        return exam;
    }

    private boolean isWithinAvailabilityWindow(Exam exam, LocalDateTime now) {
        return (exam.getStartAt() == null || !now.isBefore(exam.getStartAt()))
                && (exam.getEndAt() == null || !now.isAfter(exam.getEndAt()));
    }

    private StudentExamListResponse toListResponse(Exam exam) {
        return new StudentExamListResponse(
                exam.getId(),
                exam.getTitle(),
                exam.getDescription(),
                exam.getSubject().getName(),
                exam.getDurationMinutes(),
                exam.getStartAt(),
                exam.getEndAt());
    }

    private StudentQuestionResponse toStudentQuestion(ExamQuestion question) {
        List<StudentOptionResponse> options = question.getOptions().stream()
                .map(option -> new StudentOptionResponse(
                        option.getId(),
                        option.getOptionText(),
                        option.getOptionOrder()))
                .toList();

        return new StudentQuestionResponse(
                question.getId(),
                question.getQuestionText(),
                question.getQuestionOrder(),
                question.getMarks(),
                question.getNegativeMarks(),
                options);
    }
}
