package com.examplatform.exam.service;

import com.examplatform.user.enums.UserRole;
import com.examplatform.exam.entity.Exam;
import com.examplatform.question.entity.ExamQuestion;
import com.examplatform.exam.entity.ExamVisibility;
import com.examplatform.question.entity.QuestionOption;
import com.examplatform.exam.dto.UpdateExamRequest;
import com.examplatform.question.repository.ExamQuestionRepository;
import com.examplatform.exam.repository.ExamRepository;
import com.examplatform.subject.entity.Subject;
import com.examplatform.subject.repository.SubjectRepository;
import com.examplatform.user.entity.User;
import com.examplatform.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ExamServicePublishingTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ExamQuestionRepository questionRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private UserRepository userRepository;

    private ExamService examService;
    private Exam exam;
    private Subject subject;

    @BeforeEach
    void setUp() {
        examService = new ExamService(
                examRepository,
                questionRepository,
                subjectRepository,
                userRepository);
        User admin = new User("Admin", "admin@example.test", "hash", UserRole.ADMIN);
        subject = new Subject("Mathematics", null, admin);
        org.springframework.test.util.ReflectionTestUtils.setField(subject, "id", 3L);
        exam = new Exam();
        exam.setInitialDetails(
                "Exam",
                "Description",
                subject,
                admin,
                30,
                BigDecimal.valueOf(5),
                false,
                ExamVisibility.PRIVATE,
                LocalDateTime.of(2026, 1, 1, 9, 0),
                LocalDateTime.of(2026, 1, 1, 10, 0));
        lenient().when(examRepository.findByIdAndAdminId(12L, 7L))
                .thenReturn(Optional.of(exam));
    }

    @Test
    void doesNotPublishExamWithoutQuestions() {
        when(questionRepository.countByExamId(12L)).thenReturn(0L);

        assertThrows(
                IllegalStateException.class,
                () -> examService.publishExam(7L, 12L));
    }

    @Test
    void doesNotPublishQuestionWithInvalidOptions() {
        ExamQuestion question = validQuestion();
        question.clearOptions();
        question.addOption(new QuestionOption("Only one", true, 1));
        when(questionRepository.countByExamId(12L)).thenReturn(1L);
        when(questionRepository.findAllByExamIdOrderByQuestionOrderAsc(12L))
                .thenReturn(List.of(question));

        assertThrows(
                IllegalStateException.class,
                () -> examService.publishExam(7L, 12L));
    }

    @Test
    void publishesExamWhenEveryQuestionHasExactlyOneCorrectOption() {
        when(questionRepository.countByExamId(12L)).thenReturn(1L);
        when(questionRepository.findAllByExamIdOrderByQuestionOrderAsc(12L))
                .thenReturn(List.of(validQuestion()));
        when(examRepository.save(exam)).thenReturn(exam);

        Exam published = examService.publishExam(7L, 12L);

        assertEquals("PUBLISHED", published.getStatus().name());
        verify(examRepository).save(exam);
    }

    @Test
    void doesNotPublishExamThatIsNotOwnedByTheAdmin() {
        when(examRepository.findByIdAndAdminId(12L, 99L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> examService.publishExam(99L, 12L));
    }

    @Test
    void publishedExamVisibilityCanBeChangedInBothDirections() {
        exam.publish();
        when(examRepository.save(exam)).thenReturn(exam);

        Exam publicExam = examService.updateExam(7L, 12L, updateRequest(
                ExamVisibility.PUBLIC_LINK));
        assertEquals(ExamVisibility.PUBLIC_LINK, publicExam.getVisibility());

        Exam privateExam = examService.updateExam(7L, 12L, updateRequest(
                ExamVisibility.PRIVATE));
        assertEquals(ExamVisibility.PRIVATE, privateExam.getVisibility());
        verify(examRepository, org.mockito.Mockito.times(2)).save(exam);
        verify(subjectRepository, never()).findByIdAndAdminId(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void rejectsPublishedExamContentChanges() {
        exam.publish();
        List<UpdateExamRequest> changedRequests = List.of(
                new UpdateExamRequest(
                        "Changed title", "Description", 3L, 30, BigDecimal.valueOf(5),
                        false, ExamVisibility.PUBLIC_LINK,
                        LocalDateTime.of(2026, 1, 1, 9, 0),
                        LocalDateTime.of(2026, 1, 1, 10, 0)),
                new UpdateExamRequest(
                        "Exam", "Changed description", 3L, 30, BigDecimal.valueOf(5),
                        false, ExamVisibility.PUBLIC_LINK,
                        LocalDateTime.of(2026, 1, 1, 9, 0),
                        LocalDateTime.of(2026, 1, 1, 10, 0)),
                new UpdateExamRequest(
                        "Exam", "Description", 4L, 30, BigDecimal.valueOf(5),
                        false, ExamVisibility.PUBLIC_LINK,
                        LocalDateTime.of(2026, 1, 1, 9, 0),
                        LocalDateTime.of(2026, 1, 1, 10, 0)),
                new UpdateExamRequest(
                        "Exam", "Description", 3L, 31, BigDecimal.valueOf(5),
                        false, ExamVisibility.PUBLIC_LINK,
                        LocalDateTime.of(2026, 1, 1, 9, 0),
                        LocalDateTime.of(2026, 1, 1, 10, 0)),
                new UpdateExamRequest(
                        "Exam", "Description", 3L, 30, BigDecimal.valueOf(6),
                        false, ExamVisibility.PUBLIC_LINK,
                        LocalDateTime.of(2026, 1, 1, 9, 0),
                        LocalDateTime.of(2026, 1, 1, 10, 0)),
                new UpdateExamRequest(
                        "Exam", "Description", 3L, 30, BigDecimal.valueOf(5),
                        true, ExamVisibility.PUBLIC_LINK,
                        LocalDateTime.of(2026, 1, 1, 9, 0),
                        LocalDateTime.of(2026, 1, 1, 10, 0)),
                new UpdateExamRequest(
                        "Exam", "Description", 3L, 30, BigDecimal.valueOf(5),
                        false, ExamVisibility.PUBLIC_LINK,
                        LocalDateTime.of(2026, 1, 1, 9, 1),
                        LocalDateTime.of(2026, 1, 1, 10, 0)),
                new UpdateExamRequest(
                        "Exam", "Description", 3L, 30, BigDecimal.valueOf(5),
                        false, ExamVisibility.PUBLIC_LINK,
                        LocalDateTime.of(2026, 1, 1, 9, 0),
                        LocalDateTime.of(2026, 1, 1, 10, 1)));

        for (UpdateExamRequest request : changedRequests) {
            assertThrows(
                    IllegalStateException.class,
                    () -> examService.updateExam(7L, 12L, request));
        }

        assertEquals(ExamVisibility.PRIVATE, exam.getVisibility());
        verify(examRepository, never()).save(exam);
        verify(subjectRepository, never()).findByIdAndAdminId(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void rejectsUpdatesToArchivedExam() {
        exam.archive();

        assertThrows(
                IllegalStateException.class,
                () -> examService.updateExam(
                        7L, 12L, updateRequest(ExamVisibility.PUBLIC_LINK)));

        assertEquals(ExamVisibility.PRIVATE, exam.getVisibility());
        verify(examRepository, never()).save(exam);
    }

    @Test
    void adminCannotChangeAnotherAdminsPublishedExamVisibility() {
        exam.publish();
        when(examRepository.findByIdAndAdminId(12L, 99L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> examService.updateExam(
                        99L, 12L, updateRequest(ExamVisibility.PUBLIC_LINK)));

        assertEquals(ExamVisibility.PRIVATE, exam.getVisibility());
        verify(examRepository, never()).save(exam);
    }

    @Test
    void draftExamUpdateBehaviorRemainsUnchanged() {
        when(subjectRepository.findByIdAndAdminId(3L, 7L))
                .thenReturn(Optional.of(subject));
        when(examRepository.save(exam)).thenReturn(exam);

        Exam updated = examService.updateExam(
                7L,
                12L,
                new UpdateExamRequest(
                        "Updated draft",
                        "Updated description",
                        3L,
                        60,
                        BigDecimal.valueOf(10),
                        true,
                        ExamVisibility.PUBLIC_LINK,
                        null,
                        null));

        assertEquals("Updated draft", updated.getTitle());
        assertEquals(60, updated.getDurationMinutes());
        assertEquals(BigDecimal.valueOf(10), updated.getPassingMarks());
        assertEquals(ExamVisibility.PUBLIC_LINK, updated.getVisibility());
        verify(subjectRepository).findByIdAndAdminId(3L, 7L);
        verify(examRepository).save(exam);
    }

    private UpdateExamRequest updateRequest(ExamVisibility visibility) {
        return new UpdateExamRequest(
                "Exam",
                "Description",
                3L,
                30,
                BigDecimal.valueOf(5),
                false,
                visibility,
                LocalDateTime.of(2026, 1, 1, 9, 0),
                LocalDateTime.of(2026, 1, 1, 10, 0));
    }

    private ExamQuestion validQuestion() {
        ExamQuestion question = new ExamQuestion(
                exam,
                "Question text",
                null,
                1,
                BigDecimal.ONE,
                BigDecimal.ZERO);
        question.addOption(new QuestionOption("Correct", true, 1));
        question.addOption(new QuestionOption("Incorrect", false, 2));
        return question;
    }
}
