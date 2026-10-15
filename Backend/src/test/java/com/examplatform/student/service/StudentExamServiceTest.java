package com.examplatform.student.service;

import com.examplatform.student.dto.ExamSubmissionRequest;
import com.examplatform.student.dto.ExamSubmissionResult;
import com.examplatform.student.dto.StudentExamResponse;
import com.examplatform.student.dto.StudentResultResponse;
import com.examplatform.exam.entity.Exam;
import com.examplatform.question.entity.ExamQuestion;
import com.examplatform.result.entity.ExamResult;
import com.examplatform.exam.entity.ExamStatus;
import com.examplatform.exam.entity.ExamVisibility;
import com.examplatform.question.entity.QuestionOption;
import com.examplatform.question.repository.ExamQuestionRepository;
import com.examplatform.exam.repository.ExamRepository;
import com.examplatform.result.repository.ExamResultRepository;
import com.examplatform.user.enums.UserRole;
import com.examplatform.subject.entity.Subject;
import com.examplatform.user.entity.User;
import com.examplatform.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentExamServiceTest {

    private ExamRepository examRepository;
    private ExamQuestionRepository questionRepository;
    private ExamResultRepository resultRepository;
    private UserRepository userRepository;
    private StudentExamService studentExamService;

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        questionRepository = mock(ExamQuestionRepository.class);
        resultRepository = mock(ExamResultRepository.class);
        userRepository = mock(UserRepository.class);
        when(resultRepository.save(any(ExamResult.class))).thenAnswer(invocation -> {
            ExamResult result = invocation.getArgument(0);
            ReflectionTestUtils.setField(result, "id", 99L);
            return result;
        });
        User student = new User(
                "Student", "student@example.test", "hash", UserRole.STUDENT);
        ReflectionTestUtils.setField(student, "id", 7L);
        when(userRepository.findByEmail("student@example.test"))
                .thenReturn(Optional.of(student));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "student@example.test", "n/a", List.of()));
        studentExamService = new StudentExamService(
                examRepository,
                questionRepository,
                resultRepository,
                userRepository);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listsOnlyPublishedPublicLinkExams() {
        Exam exam = exam(1L, ExamVisibility.PUBLIC_LINK, true, BigDecimal.valueOf(4));
        when(examRepository.findAllByStatusAndVisibilityOrderByCreatedAtDesc(
                ExamStatus.PUBLISHED,
                ExamVisibility.PUBLIC_LINK))
                .thenReturn(List.of(exam));

        var result = studentExamService.listAvailableExams();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        verify(examRepository).findAllByStatusAndVisibilityOrderByCreatedAtDesc(
                ExamStatus.PUBLISHED,
                ExamVisibility.PUBLIC_LINK);
    }

    @Test
    void refusesPrivateOrUnpublishedExamsEvenWhenIdIsKnown() {
        when(examRepository.findByIdAndStatusAndVisibility(
                12L,
                ExamStatus.PUBLISHED,
                ExamVisibility.PUBLIC_LINK))
                .thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> studentExamService.getExam(12L));
        assertThrows(
                ResponseStatusException.class,
                () -> studentExamService.submitAnswers(
                        12L,
                        new ExamSubmissionRequest(List.of())));
    }

    @Test
    void studentExamResponseOmitsCorrectAnswersAndExplanations() {
        Exam exam = exam(1L, ExamVisibility.PUBLIC_LINK, true, BigDecimal.valueOf(4));
        ExamQuestion question = question(exam, 10L, 1, "Prompt", "private rationale",
                "5", "2", 101L, 102L);
        when(examRepository.findByIdAndStatusAndVisibility(
                1L,
                ExamStatus.PUBLISHED,
                ExamVisibility.PUBLIC_LINK))
                .thenReturn(Optional.of(exam));
        when(questionRepository.findAllByExamIdOrderByQuestionOrderAsc(1L))
                .thenReturn(List.of(question));

        StudentExamResponse response = studentExamService.getExam(1L);

        assertEquals(45, response.durationMinutes());
        assertEquals(2, response.questions().get(0).options().size());
        assertFalse(hasRecordComponent(response.questions().get(0), "explanation"));
        assertFalse(hasRecordComponent(response.questions().get(0).options().get(0), "correct"));
    }

    @Test
    void scoresCorrectIncorrectAndUnansweredAndPersistsOnlySummary() {
        Exam exam = exam(1L, ExamVisibility.PUBLIC_LINK, true, BigDecimal.valueOf(4));
        ExamQuestion first = question(exam, 10L, 1, "First", null,
                "5", "2", 101L, 102L);
        ExamQuestion second = question(exam, 20L, 2, "Second", null,
                "3", "1", 201L, 202L);
        ExamQuestion third = question(exam, 30L, 3, "Third", null,
                "2", "0.5", 301L, 302L);
        when(examRepository.findByIdAndStatusAndVisibility(
                1L,
                ExamStatus.PUBLISHED,
                ExamVisibility.PUBLIC_LINK))
                .thenReturn(Optional.of(exam));
        when(questionRepository.findAllByExamIdOrderByQuestionOrderAsc(1L))
                .thenReturn(List.of(first, second, third));

        ExamSubmissionResult result = studentExamService.submitAnswers(
                1L,
                new ExamSubmissionRequest(List.of(
                        new ExamSubmissionRequest.SubmittedAnswer(10L, 101L),
                        new ExamSubmissionRequest.SubmittedAnswer(20L, 202L))));

        assertEquals(3, result.totalQuestions());
        assertEquals(99L, result.resultId());
        assertEquals(1, result.correctAnswers());
        assertEquals(1, result.incorrectAnswers());
        assertEquals(1, result.unansweredQuestions());
        assertEquals(0, new BigDecimal("4.00").compareTo(result.score()));
        assertEquals(Boolean.TRUE, result.passed());
        assertFalse(hasRecordComponent(result, "answers"));
        assertFalse(hasRecordComponent(result, "correctOptionId"));
        assertFalse(hasRecordComponent(result, "explanation"));
        verify(examRepository, never()).save(any());
        verify(questionRepository, never()).save(any());
        var savedResult = forClass(ExamResult.class);
        verify(resultRepository).save(savedResult.capture());
        assertEquals(7L, savedResult.getValue().getStudent().getId());
        assertEquals(1L, savedResult.getValue().getExam().getId());
        assertEquals(0, new BigDecimal("4.00").compareTo(savedResult.getValue().getScore()));
        assertEquals(0, new BigDecimal("10.00")
                .compareTo(savedResult.getValue().getMaximumMarks()));
        assertTrue(savedResult.getValue().isPassed());
        assertFalse(hasField(savedResult.getValue(), "answers"));
    }

    @Test
    void acceptsExplicitlyUnansweredQuestionAndReturnsNoPassWhenNoThreshold() {
        Exam exam = exam(1L, ExamVisibility.PUBLIC_LINK, true, null);
        ExamQuestion question = question(exam, 10L, 1, "First", null,
                "5", "2", 101L, 102L);
        when(examRepository.findByIdAndStatusAndVisibility(
                1L,
                ExamStatus.PUBLISHED,
                ExamVisibility.PUBLIC_LINK))
                .thenReturn(Optional.of(exam));
        when(questionRepository.findAllByExamIdOrderByQuestionOrderAsc(1L))
                .thenReturn(List.of(question));

        ExamSubmissionResult result = studentExamService.submitAnswers(
                1L,
                new ExamSubmissionRequest(List.of(
                        new ExamSubmissionRequest.SubmittedAnswer(10L, null))));

        assertEquals(1, result.unansweredQuestions());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.score()));
        assertNull(result.passed());
        var savedResult = forClass(ExamResult.class);
        verify(resultRepository).save(savedResult.capture());
        assertFalse(savedResult.getValue().isPassed());
    }

    @Test
    void rejectsDuplicateOrForeignQuestionsAndOptions() {
        Exam exam = exam(1L, ExamVisibility.PUBLIC_LINK, true, null);
        ExamQuestion question = question(exam, 10L, 1, "First", null,
                "5", "2", 101L, 102L);
        when(examRepository.findByIdAndStatusAndVisibility(
                1L,
                ExamStatus.PUBLISHED,
                ExamVisibility.PUBLIC_LINK))
                .thenReturn(Optional.of(exam));
        when(questionRepository.findAllByExamIdOrderByQuestionOrderAsc(1L))
                .thenReturn(List.of(question));

        assertThrows(IllegalArgumentException.class, () ->
                studentExamService.submitAnswers(1L, new ExamSubmissionRequest(List.of(
                        new ExamSubmissionRequest.SubmittedAnswer(10L, 101L),
                        new ExamSubmissionRequest.SubmittedAnswer(10L, 102L)))));
        assertThrows(IllegalArgumentException.class, () ->
                studentExamService.submitAnswers(1L, new ExamSubmissionRequest(List.of(
                        new ExamSubmissionRequest.SubmittedAnswer(99L, 101L)))));
        assertThrows(IllegalArgumentException.class, () ->
                studentExamService.submitAnswers(1L, new ExamSubmissionRequest(List.of(
                        new ExamSubmissionRequest.SubmittedAnswer(10L, 999L)))));
        verify(resultRepository, never()).save(any());
    }

    @Test
    void allowsUnlimitedRepeatedSubmissions() {
        Exam exam = exam(1L, ExamVisibility.PUBLIC_LINK, true, BigDecimal.ONE);
        ExamQuestion question = question(exam, 10L, 1, "First", null,
                "1", "0", 101L, 102L);
        when(examRepository.findByIdAndStatusAndVisibility(
                1L,
                ExamStatus.PUBLISHED,
                ExamVisibility.PUBLIC_LINK))
                .thenReturn(Optional.of(exam));
        when(questionRepository.findAllByExamIdOrderByQuestionOrderAsc(1L))
                .thenReturn(List.of(question));
        var request = new ExamSubmissionRequest(List.of(
                new ExamSubmissionRequest.SubmittedAnswer(10L, 101L)));

        studentExamService.submitAnswers(1L, request);
        studentExamService.submitAnswers(1L, request);

        verify(resultRepository, org.mockito.Mockito.times(2)).save(any(ExamResult.class));
    }

    @Test
    void retrievesResultOnlyWhenItBelongsToAuthenticatedStudent() {
        Exam exam = exam(1L, ExamVisibility.PUBLIC_LINK, true, BigDecimal.ONE);
        ExamResult result = new ExamResult(
                student(),
                exam,
                BigDecimal.ONE,
                BigDecimal.TEN,
                true);
        ReflectionTestUtils.setField(result, "id", 91L);
        when(resultRepository.findByIdAndStudent_Id(91L, 7L))
                .thenReturn(Optional.of(result));

        StudentResultResponse response = studentExamService.getResult(91L);

        assertEquals(91L, response.resultId());
        assertEquals(1L, response.examId());
        assertEquals("Practice exam", response.examTitle());
        assertEquals("Mathematics", response.subjectName());
        assertEquals(BigDecimal.ONE, response.score());
        assertEquals(BigDecimal.TEN, response.maximumMarks());
        assertTrue(response.passed());
        verify(resultRepository).findByIdAndStudent_Id(91L, 7L);
    }

    @Test
    void doesNotRevealResultOwnedByAnotherStudent() {
        when(resultRepository.findByIdAndStudent_Id(91L, 7L))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> studentExamService.getResult(91L));

        assertEquals(404, error.getStatusCode().value());
        verify(resultRepository).findByIdAndStudent_Id(91L, 7L);
    }

    private User student() {
        User student = new User(
                "Student", "student@example.test", "hash", UserRole.STUDENT);
        ReflectionTestUtils.setField(student, "id", 7L);
        return student;
    }

    private Exam exam(
            Long id,
            ExamVisibility visibility,
            boolean published,
            BigDecimal passingMarks) {
        User admin = new User("Admin", "admin@example.test", "hash", UserRole.ADMIN);
        Exam exam = new Exam();
        exam.setInitialDetails(
                "Practice exam",
                "Description",
                new Subject("Mathematics", null, admin),
                admin,
                45,
                passingMarks,
                false,
                visibility,
                null,
                null);
        if (published) {
            exam.publish();
        }
        ReflectionTestUtils.setField(exam, "id", id);
        return exam;
    }

    private ExamQuestion question(
            Exam exam,
            Long questionId,
            int order,
            String text,
            String explanation,
            String marks,
            String negativeMarks,
            Long correctOptionId,
            Long wrongOptionId) {
        ExamQuestion question = new ExamQuestion(
                exam,
                text,
                explanation,
                order,
                new BigDecimal(marks),
                new BigDecimal(negativeMarks));
        QuestionOption correct = new QuestionOption("Correct choice", true, 1);
        QuestionOption wrong = new QuestionOption("Wrong choice", false, 2);
        ReflectionTestUtils.setField(correct, "id", correctOptionId);
        ReflectionTestUtils.setField(wrong, "id", wrongOptionId);
        question.addOption(correct);
        question.addOption(wrong);
        ReflectionTestUtils.setField(question, "id", questionId);
        return question;
    }

    private boolean hasRecordComponent(Object value, String componentName) {
        return java.util.Arrays.stream(value.getClass().getRecordComponents())
                .anyMatch(component -> component.getName().equals(componentName));
    }

    private boolean hasField(Object value, String fieldName) {
        return java.util.Arrays.stream(value.getClass().getDeclaredFields())
                .anyMatch(field -> field.getName().equals(fieldName));
    }
}
