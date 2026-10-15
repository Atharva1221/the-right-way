package com.examplatform.student.service;

import com.examplatform.user.enums.UserRole;
import com.examplatform.student.dto.StudentDashboardResponse;
import com.examplatform.exam.entity.Exam;
import com.examplatform.result.entity.ExamResult;
import com.examplatform.result.repository.ExamResultRepository;
import com.examplatform.subject.entity.Subject;
import com.examplatform.user.entity.User;
import com.examplatform.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentDashboardServiceTest {

    private ExamResultRepository resultRepository;
    private UserRepository userRepository;
    private StudentDashboardService dashboardService;
    private User student;

    @BeforeEach
    void setUp() {
        resultRepository = mock(ExamResultRepository.class);
        userRepository = mock(UserRepository.class);
        student = new User(
                "Student", "student@example.test", "hash", UserRole.STUDENT);
        ReflectionTestUtils.setField(student, "id", 7L);
        when(userRepository.findByEmail("student@example.test"))
                .thenReturn(Optional.of(student));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "student@example.test", "n/a", List.of()));
        dashboardService = new StudentDashboardService(resultRepository, userRepository);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void calculatesTotalsPassRateAndSubjectAveragesFromOnlyCurrentStudentResults() {
        Subject mathematics = subject(11L, "Mathematics");
        Subject physics = subject(12L, "Physics");
        List<ExamResult> results = List.of(
                result(exam(21L, mathematics), "7", "10", true),
                result(exam(22L, mathematics), "2", "4", false),
                result(exam(23L, physics), "0", "0", true));
        when(resultRepository.findAllByStudentIdOrderBySubmittedAtDesc(7L))
                .thenReturn(results);

        StudentDashboardResponse response = dashboardService.getDashboard();

        assertEquals(3, response.testsTaken());
        assertEquals(2, response.testsPassed());
        assertEquals(1, response.testsFailed());
        assertEquals(new BigDecimal("66.67"), response.passRate());
        assertEquals(2, response.subjectPerformance().size());
        var math = response.subjectPerformance().get(0);
        assertEquals(11L, math.subjectId());
        assertEquals("Mathematics", math.subjectName());
        assertEquals(2, math.testsTaken());
        assertEquals(1, math.testsPassed());
        assertEquals(1, math.testsFailed());
        assertEquals(new BigDecimal("60.00"), math.averageScorePercentage());
        var physicsPerformance = response.subjectPerformance().get(1);
        assertEquals("Physics", physicsPerformance.subjectName());
        assertEquals(
                new BigDecimal("0.00"),
                physicsPerformance.averageScorePercentage());
        verify(resultRepository).findAllByStudentIdOrderBySubmittedAtDesc(7L);
    }

    @Test
    void returnsZeroMetricsWhenStudentHasNoSubmissions() {
        when(resultRepository.findAllByStudentIdOrderBySubmittedAtDesc(7L))
                .thenReturn(List.of());

        StudentDashboardResponse response = dashboardService.getDashboard();

        assertEquals(0, response.testsTaken());
        assertEquals(0, response.testsPassed());
        assertEquals(0, response.testsFailed());
        assertEquals(new BigDecimal("0.00"), response.passRate());
        assertEquals(List.of(), response.subjectPerformance());
        verify(resultRepository).findAllByStudentIdOrderBySubmittedAtDesc(7L);
    }

    private Subject subject(Long id, String name) {
        User admin = new User("Admin", "admin@example.test", "hash", UserRole.ADMIN);
        Subject subject = new Subject(name, null, admin);
        ReflectionTestUtils.setField(subject, "id", id);
        return subject;
    }

    private Exam exam(Long id, Subject subject) {
        User admin = subject.getAdmin();
        Exam exam = new Exam();
        exam.setInitialDetails(
                "Exam", null, subject, admin, 30, null, false, null, null, null);
        ReflectionTestUtils.setField(exam, "id", id);
        return exam;
    }

    private ExamResult result(
            Exam exam,
            String score,
            String maximumMarks,
            boolean passed) {
        return new ExamResult(
                student,
                exam,
                new BigDecimal(score),
                new BigDecimal(maximumMarks),
                passed);
    }
}
