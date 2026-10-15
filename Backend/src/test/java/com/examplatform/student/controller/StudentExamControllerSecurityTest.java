package com.examplatform.student.controller;

import com.examplatform.student.dto.ExamSubmissionRequest;
import com.examplatform.student.dto.ExamSubmissionResult;
import com.examplatform.student.dto.StudentDashboardResponse;
import com.examplatform.student.dto.StudentExamListResponse;
import com.examplatform.student.dto.StudentSubjectPerformance;
import com.examplatform.student.dto.StudentExamResponse;
import com.examplatform.student.dto.StudentResultResponse;
import com.examplatform.student.service.StudentDashboardService;
import com.examplatform.student.service.StudentExamService;
import java.math.BigDecimal;
import java.util.List;
import com.examplatform.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "cors.allowed-origins=https://app.example.test")
class StudentExamControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private StudentExamService studentExamService;

    @MockitoBean
    private StudentDashboardService dashboardService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void corsAllowsOnlyConfiguredOriginAndRequiredHeaders() throws Exception {
        mockMvc.perform(options("/api/student/dashboard")
                        .header("Origin", "https://app.example.test")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin",
                        "https://app.example.test"))
                .andExpect(header().string(
                        "Access-Control-Allow-Credentials",
                        "true"));

        mockMvc.perform(options("/api/student/dashboard")
                        .header("Origin", "https://untrusted.example.test")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthEndpointIsPublicAndReportsDatabaseAvailability() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"));
    }

    @Test
    void studentCanRetrieveDashboardMetrics() throws Exception {
        when(dashboardService.getDashboard())
                .thenReturn(new StudentDashboardResponse(
                        2,
                        1,
                        1,
                        new BigDecimal("50.00"),
                        List.of(new StudentSubjectPerformance(
                                4L,
                                "Mathematics",
                                2,
                                1,
                                1,
                                new BigDecimal("72.50")))));

        mockMvc.perform(get("/api/student/dashboard")
                        .with(user("student@example.test").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testsTaken").value(2))
                .andExpect(jsonPath("$.testsPassed").value(1))
                .andExpect(jsonPath("$.testsFailed").value(1))
                .andExpect(jsonPath("$.passRate").value(50))
                .andExpect(jsonPath("$.subjectPerformance[0].subjectId").value(4))
                .andExpect(jsonPath(
                        "$.subjectPerformance[0].averageScorePercentage").value(72.5));

        verify(dashboardService).getDashboard();
    }

    @Test
    void studentCanListAndRetrieveStudentSafeExamContent() throws Exception {
        when(studentExamService.listAvailableExams())
                .thenReturn(List.of(new StudentExamListResponse(
                        1L, "Practice", "Description", "Math", 45, null, null)));
        when(studentExamService.getExam(1L))
                .thenReturn(new StudentExamResponse(
                        1L, "Practice", "Description", "Math", 45, null, null, List.of()));

        mockMvc.perform(get("/api/student/exams")
                        .with(user("student@example.test").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
        mockMvc.perform(get("/api/student/exams/1")
                        .with(user("student@example.test").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions").isArray());

        verify(studentExamService).listAvailableExams();
        verify(studentExamService).getExam(1L);
    }

    @Test
    void rejectsUnauthenticatedAndAdminUsersFromStudentEndpoints()
            throws Exception {
        mockMvc.perform(get("/api/student/exams"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/student/exams")
                        .with(user("admin@example.test").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/student/dashboard"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/student/dashboard")
                        .with(user("admin@example.test").roles("ADMIN")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(studentExamService, dashboardService);
    }

    @Test
    void validJwtForWrongRoleIsForbiddenWhileAnonymousRequestIsUnauthorized()
            throws Exception {
        UserDetails admin = User.withUsername("admin@example.test")
                .password("not-used")
                .authorities("ROLE_ADMIN")
                .build();
        when(userDetailsService.loadUserByUsername("admin@example.test"))
                .thenReturn(admin);
        String adminToken = jwtService.generateToken(admin);

        mockMvc.perform(get("/api/student/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/student/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentSubmissionRequiresCsrfAndIsPassedToScoringService()
            throws Exception {
        ExamSubmissionResult result = new ExamSubmissionResult(
                9L, 1L, 1, 1, 0, 0, BigDecimal.ONE, BigDecimal.ONE, true);
        when(studentExamService.submitAnswers(
                eq(1L),
                org.mockito.ArgumentMatchers.any(ExamSubmissionRequest.class)))
                .thenReturn(result);
        String requestBody = """
                {"answers":[{"questionId":11,"selectedOptionId":110}]}
                """;

        mockMvc.perform(post("/api/student/exams/1/submissions")
                        .with(user("student@example.test").roles("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/student/exams/1/submissions")
                        .with(user("student@example.test").roles("STUDENT"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultId").value(9))
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.correctAnswers").value(1));

        verify(studentExamService).submitAnswers(
                eq(1L),
                org.mockito.ArgumentMatchers.any(ExamSubmissionRequest.class));
    }

    @Test
    void studentCanRetrieveOwnResultButAdminAndAnonymousCannot() throws Exception {
        when(studentExamService.getResult(9L))
                .thenReturn(new StudentResultResponse(
                        9L,
                        1L,
                        "Practice",
                        "Mathematics",
                        BigDecimal.ONE,
                        BigDecimal.TEN,
                        true,
                        java.time.LocalDateTime.parse("2026-10-15T12:00:00")));

        mockMvc.perform(get("/api/student/results/9")
                        .with(user("student@example.test").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultId").value(9))
                .andExpect(jsonPath("$.maximumMarks").value(10))
                .andExpect(jsonPath("$.passed").value(true));
        mockMvc.perform(get("/api/student/results/9"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/student/results/9")
                        .with(user("admin@example.test").roles("ADMIN")))
                .andExpect(status().isForbidden());

        verify(studentExamService).getResult(9L);
    }

    @Test
    void adminCannotSubmitAnswersToStudentEndpoint() throws Exception {
        mockMvc.perform(post("/api/student/exams/1/submissions")
                        .with(user("admin@example.test").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers":[]}
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(studentExamService);
    }
}
