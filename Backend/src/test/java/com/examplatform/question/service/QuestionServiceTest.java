package com.examplatform.question.service;

import com.examplatform.user.enums.UserRole;
import com.examplatform.question.dto.CreateQuestionRequest;
import com.examplatform.question.dto.QuestionOptionRequest;
import com.examplatform.question.dto.UpdateQuestionRequest;
import com.examplatform.exam.entity.Exam;
import com.examplatform.question.entity.ExamQuestion;
import com.examplatform.exam.entity.ExamVisibility;
import com.examplatform.question.entity.QuestionOption;
import com.examplatform.question.repository.ExamQuestionRepository;
import com.examplatform.exam.repository.ExamRepository;
import com.examplatform.subject.entity.Subject;
import com.examplatform.user.entity.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ExamQuestionRepository questionRepository;

    private QuestionService questionService;
    private Exam exam;

    @BeforeEach
    void setUp() {
        questionService = new QuestionService(examRepository, questionRepository);
        User admin = new User("Admin", "admin@example.test", "hash", UserRole.ADMIN);
        Subject subject = new Subject("Math", null, admin);
        exam = new Exam();
        exam.setInitialDetails(
                "Exam",
                null,
                subject,
                admin,
                30,
                null,
                false,
                ExamVisibility.PRIVATE,
                null,
                null);
        lenient().when(examRepository.findByIdAndAdminId(11L, 7L))
                .thenReturn(Optional.of(exam));
    }

    @Test
    void createsQuestionWithOptionsBelongingToTheQuestion() {
        when(questionRepository.existsByExamIdAndQuestionOrder(11L, 1))
                .thenReturn(false);
        when(questionRepository.save(any(ExamQuestion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ExamQuestion created = questionService.createQuestion(
                7L,
                11L,
                createRequest());

        assertEquals("Question text", created.getQuestionText());
        assertEquals(2, created.getOptions().size());
        assertEquals(List.of(1, 2), created.getOptions().stream()
                .map(QuestionOption::getOptionOrder)
                .toList());
        assertEquals(1, created.getOptions().stream()
                .filter(QuestionOption::isCorrect)
                .count());
        created.getOptions().forEach(option ->
                assertEquals(created, option.getQuestion()));
    }

    @Test
    void readsOnlyQuestionsScopedToTheOwnedExamAndInRepositoryOrder() {
        ExamQuestion first = question(1);
        ExamQuestion second = question(2);
        when(questionRepository.findAllByExamIdOrderByQuestionOrderAsc(11L))
                .thenReturn(List.of(first, second));

        List<ExamQuestion> questions = questionService.getQuestions(7L, 11L);

        assertEquals(List.of(first, second), questions);
        verify(questionRepository)
                .findAllByExamIdOrderByQuestionOrderAsc(11L);
    }

    @Test
    void updatesQuestionAndReplacesItsOptions() {
        ExamQuestion question = question(1);
        question.addOption(new QuestionOption("Old one", true, 1));
        question.addOption(new QuestionOption("Old two", false, 2));
        when(questionRepository.findByIdAndExamId(18L, 11L))
                .thenReturn(Optional.of(question));
        when(questionRepository
                .existsByExamIdAndQuestionOrderAndIdNot(11L, 2, 18L))
                .thenReturn(false);
        when(questionRepository.save(question)).thenReturn(question);

        ExamQuestion updated = questionService.updateQuestion(
                7L,
                11L,
                18L,
                new UpdateQuestionRequest(
                        "Updated text",
                        "Updated explanation",
                        2,
                        new BigDecimal("2.00"),
                        new BigDecimal("0.25"),
                        List.of(
                                new QuestionOptionRequest("New first", false, 1),
                                new QuestionOptionRequest("New second", true, 2))));

        assertEquals("Updated text", updated.getQuestionText());
        assertEquals("Updated explanation", updated.getExplanation());
        assertEquals(2, updated.getQuestionOrder());
        assertEquals(2, updated.getOptions().size());
        assertEquals("New first", updated.getOptions().get(0).getOptionText());
        assertEquals("New second", updated.getOptions().get(1).getOptionText());
        verify(questionRepository).flush();
    }

    @Test
    void refusesQuestionsFromAnotherExamOrOwnedByAnotherAdmin() {
        when(questionRepository.findByIdAndExamId(18L, 11L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> questionService.getQuestion(7L, 11L, 18L));

        when(examRepository.findByIdAndAdminId(11L, 99L))
                .thenReturn(Optional.empty());
        assertThrows(
                IllegalArgumentException.class,
                () -> questionService.getQuestions(99L, 11L));
    }

    @Test
    void rejectsDuplicateQuestionOrderAndInvalidOptionOrders() {
        when(questionRepository.existsByExamIdAndQuestionOrder(11L, 1))
                .thenReturn(true);
        assertThrows(
                IllegalArgumentException.class,
                () -> questionService.createQuestion(7L, 11L, createRequest()));

        CreateQuestionRequest duplicateOptionOrder = new CreateQuestionRequest(
                "Question text",
                null,
                2,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                List.of(
                        new QuestionOptionRequest("First", true, 1),
                        new QuestionOptionRequest("Second", false, 1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> questionService.createQuestion(7L, 11L, duplicateOptionOrder));
    }

    @Test
    void preventsEditingQuestionsAfterExamIsPublished() {
        exam.publish();

        assertThrows(
                IllegalStateException.class,
                () -> questionService.createQuestion(7L, 11L, createRequest()));
    }

    @Test
    void deletesQuestionOnlyAfterExamOwnershipAndDraftChecks() {
        ExamQuestion question = question(1);
        when(questionRepository.findByIdAndExamId(18L, 11L))
                .thenReturn(Optional.of(question));

        questionService.deleteQuestion(7L, 11L, 18L);

        verify(questionRepository).delete(question);
    }

    private CreateQuestionRequest createRequest() {
        return new CreateQuestionRequest(
                "Question text",
                null,
                1,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                List.of(
                        new QuestionOptionRequest("Second", false, 2),
                        new QuestionOptionRequest("First", true, 1)));
    }

    private ExamQuestion question(int order) {
        return new ExamQuestion(
                exam,
                "Question " + order,
                null,
                order,
                BigDecimal.ONE,
                BigDecimal.ZERO);
    }
}
