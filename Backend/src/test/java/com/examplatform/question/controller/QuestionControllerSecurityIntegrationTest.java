package com.examplatform.question.controller;

import com.examplatform.user.enums.UserRole;
import com.examplatform.question.dto.CreateQuestionRequest;
import com.examplatform.exam.entity.Exam;
import com.examplatform.question.entity.ExamQuestion;
import com.examplatform.question.entity.QuestionOption;
import com.examplatform.question.service.QuestionService;
import com.examplatform.subject.entity.Subject;
import com.examplatform.user.entity.User;
import com.examplatform.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class QuestionControllerSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestionService questionService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/exams/12/questions"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userRepository, questionService);
    }

    @Test
    void authenticatedStudentCannotReadAdminQuestionEndpoint() throws Exception {
        when(userRepository.findByEmail("student@example.test"))
                .thenReturn(Optional.of(new User(
                        "Student",
                        "student@example.test",
                        "hash",
                        UserRole.STUDENT)));

        mockMvc.perform(get("/api/exams/12/questions")
                        .with(user("student@example.test").roles("STUDENT")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(questionService);
    }

    @Test
    void adminCanCreateQuestionAndClientCannotOverrideAdminIdentity()
            throws Exception {
        User admin = new User(
                "Admin",
                "admin@example.test",
                "hash",
                UserRole.ADMIN);
        ReflectionTestUtils.setField(admin, "id", 7L);
        when(userRepository.findByEmail("admin@example.test"))
                .thenReturn(Optional.of(admin));
        when(questionService.createQuestion(
                eq(7L),
                eq(12L),
                any(CreateQuestionRequest.class)))
                .thenReturn(validQuestion());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/exams/12/questions")
                        .with(user("admin@example.test").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "adminId": 999,
                                  "questionText": "Question text",
                                  "explanation": "Admin-only explanation",
                                  "questionOrder": 1,
                                  "marks": 1,
                                  "negativeMarks": 0,
                                  "options": [
                                    {"optionText": "Correct", "correct": true, "optionOrder": 1},
                                    {"optionText": "Incorrect", "correct": false, "optionOrder": 2}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.explanation").value("Admin-only explanation"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.options[0].correct").value(true));

        verify(questionService).createQuestion(
                eq(7L),
                eq(12L),
                any(CreateQuestionRequest.class));
    }

    @Test
    void invalidCreateRequestReturnsClearValidationError() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/exams/12/questions")
                        .with(user("admin@example.test").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionText": " ",
                                  "questionOrder": 1,
                                  "marks": -1,
                                  "negativeMarks": 0,
                                  "options": [
                                    {"optionText": "Only option", "correct": true, "optionOrder": 1}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.error").isNotEmpty());

        verifyNoInteractions(questionService);
    }

    @Test
    void malformedCreateRequestReturnsBadRequestInsteadOfAuthenticationError()
            throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/exams/12/questions")
                        .with(user("admin@example.test").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionText": "Question text",
                                  "questionOrder": 1,
                                  "marks": 1,
                                  "negativeMarks": 0,
                                  "options": [
                                    {"optionText": "Correct", "correct": null, "optionOrder": 1},
                                    {"optionText": "Incorrect", "correct": false, "optionOrder": 2}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.error").value("Malformed request body"));

        verifyNoInteractions(userRepository, questionService);
    }

    private ExamQuestion validQuestion() {
        User admin = new User("Admin", "admin@example.test", "hash", UserRole.ADMIN);
        Exam exam = new Exam();
        exam.setInitialDetails(
                "Exam",
                null,
                new Subject("Subject", null, admin),
                admin,
                30,
                null,
                false,
                com.examplatform.exam.entity.ExamVisibility.PRIVATE,
                null,
                null);
        ExamQuestion question = new ExamQuestion(
                exam,
                "Question text",
                "Admin-only explanation",
                1,
                BigDecimal.ONE,
                BigDecimal.ZERO);
        question.addOption(new QuestionOption("Correct", true, 1));
        question.addOption(new QuestionOption("Incorrect", false, 2));
        return question;
    }
}
