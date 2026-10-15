package com.examplatform.exam.service;

import com.examplatform.exam.dto.CreateExamRequest;
import com.examplatform.exam.dto.UpdateExamRequest;
import com.examplatform.exam.entity.Exam;
import com.examplatform.question.entity.ExamQuestion;
import com.examplatform.exam.entity.ExamStatus;

import com.examplatform.question.repository.ExamQuestionRepository;
import com.examplatform.exam.repository.ExamRepository;
import com.examplatform.subject.entity.Subject;
import com.examplatform.subject.repository.SubjectRepository;
import com.examplatform.user.entity.User;
import com.examplatform.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    public ExamService(
            ExamRepository examRepository,
            ExamQuestionRepository questionRepository,
            SubjectRepository subjectRepository,
            UserRepository userRepository) {
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Exam createExam(Long adminId, CreateExamRequest request) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        if (!"ADMIN".equals(admin.getRole().name())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only admins can create exams");
        }

        Subject subject = subjectRepository
                .findByIdAndAdminId(request.subjectId(), adminId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

        validateExamSettings(
                request.durationMinutes(),
                request.passingMarks(),
                request.startAt(),
                request.endAt());

        Exam exam = new Exam();
        exam.setInitialDetails(
                request.title(),
                request.description(),
                subject,
                admin,
                request.durationMinutes(),
                request.passingMarks(),
                request.shuffleQuestions(),
                request.visibility(),
                request.startAt(),
                request.endAt());

        return examRepository.save(exam);
    }

    @Transactional(readOnly = true)
    public List<Exam> getExams(Long adminId) {
        return examRepository.findAllByAdminIdOrderByCreatedAtDesc(adminId);
    }

    @Transactional(readOnly = true)
    public Exam getExam(Long adminId, Long examId) {
        return examRepository.findByIdAndAdminId(examId, adminId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));
    }

    @Transactional
    public Exam updateExam(
            Long adminId,
            Long examId,
            UpdateExamRequest request) {
        Exam exam = getExam(adminId, examId);

        if (exam.getStatus() == ExamStatus.PUBLISHED) {
            if (!hasUnchangedPublishedDetails(exam, request)) {
                throw new IllegalStateException(
                        "Only exam visibility can be changed after publishing");
            }
            exam.updateVisibility(request.visibility());
            return examRepository.save(exam);
        }

        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw new IllegalStateException("Only draft exams can be edited");
        }

        Subject subject = subjectRepository
                .findByIdAndAdminId(request.subjectId(), adminId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

        validateExamSettings(
                request.durationMinutes(),
                request.passingMarks(),
                request.startAt(),
                request.endAt());

        exam.updateDetails(
                request.title(),
                request.description(),
                subject,
                request.durationMinutes(),
                request.passingMarks(),
                request.shuffleQuestions(),
                request.visibility(),
                request.startAt(),
                request.endAt());

        return examRepository.save(exam);
    }

    private boolean hasUnchangedPublishedDetails(
            Exam exam,
            UpdateExamRequest request) {
        return Objects.equals(request.title(), exam.getTitle())
                && Objects.equals(request.description(), exam.getDescription())
                && Objects.equals(request.subjectId(), exam.getSubject().getId())
                && Objects.equals(request.durationMinutes(), exam.getDurationMinutes())
                && decimalEquals(request.passingMarks(), exam.getPassingMarks())
                && request.shuffleQuestions() == exam.isShuffleQuestions()
                && Objects.equals(request.startAt(), exam.getStartAt())
                && Objects.equals(request.endAt(), exam.getEndAt());
    }

    private boolean decimalEquals(
            java.math.BigDecimal first,
            java.math.BigDecimal second) {
        return first == null
                ? second == null
                : second != null && first.compareTo(second) == 0;
    }

    @Transactional
    public void deleteExam(Long adminId, Long examId) {
        Exam exam = getExam(adminId, examId);

        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw new IllegalStateException("Only draft exams can be deleted");
        }

        examRepository.delete(exam);
    }

    @Transactional
    public Exam publishExam(Long adminId, Long examId) {
        Exam exam = getExam(adminId, examId);

        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw new IllegalStateException("Only draft exams can be published");
        }

        if (exam.getEndAt() != null
                && exam.getStartAt() != null
                && !exam.getEndAt().isAfter(exam.getStartAt())) {
            throw new IllegalArgumentException(
                    "End time must be after start time");
        }

        if (questionRepository.countByExamId(examId) == 0) {
            throw new IllegalStateException(
                    "An exam must contain at least one question before publishing");
        }
        List<ExamQuestion> questions =
                questionRepository.findAllByExamIdOrderByQuestionOrderAsc(examId);
        if (questions.stream().anyMatch(question -> !question.hasValidContent())) {
            throw new IllegalStateException(
                    "Every question must have valid text, marks, and options "
                            + "with exactly one correct option before publishing");
        }

        exam.publish();
        return examRepository.save(exam);
    }

    @Transactional
    public Exam archiveExam(Long adminId, Long examId) {
        Exam exam = getExam(adminId, examId);
        exam.archive();
        return examRepository.save(exam);
    }

    private void validateExamSettings(
            Integer durationMinutes,
            java.math.BigDecimal passingMarks,
            LocalDateTime startAt,
            LocalDateTime endAt) {

        if (durationMinutes == null || durationMinutes <= 0) {
            throw new IllegalArgumentException(
                    "Duration must be greater than zero");
        }

        if (passingMarks != null && passingMarks.signum() < 0) {
            throw new IllegalArgumentException(
                    "Passing marks cannot be negative");
        }

        if (startAt != null && endAt != null
                && !endAt.isAfter(startAt)) {
            throw new IllegalArgumentException(
                    "End time must be after start time");
        }
    }
}