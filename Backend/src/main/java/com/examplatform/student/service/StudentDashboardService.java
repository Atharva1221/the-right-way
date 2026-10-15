package com.examplatform.student.service;

import com.examplatform.student.dto.StudentDashboardResponse;
import com.examplatform.student.dto.StudentSubjectPerformance;
import com.examplatform.result.entity.ExamResult;
import com.examplatform.result.repository.ExamResultRepository;
import com.examplatform.user.entity.User;
import com.examplatform.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StudentDashboardService {

    private static final int PERCENTAGE_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final ExamResultRepository resultRepository;
    private final UserRepository userRepository;

    public StudentDashboardService(
            ExamResultRepository resultRepository,
            UserRepository userRepository) {
        this.resultRepository = resultRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public StudentDashboardResponse getDashboard() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required");
        }
        User student = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated student was not found"));
        List<ExamResult> results =
                resultRepository.findAllByStudentIdOrderBySubmittedAtDesc(student.getId());

        long testsTaken = results.size();
        long testsPassed = results.stream().filter(ExamResult::isPassed).count();
        long testsFailed = testsTaken - testsPassed;
        BigDecimal passRate = percentage(testsPassed, testsTaken);

        Map<Long, List<ExamResult>> resultsBySubject = new LinkedHashMap<>();
        for (ExamResult result : results) {
            Long subjectId = result.getExam().getSubject().getId();
            resultsBySubject.computeIfAbsent(subjectId, ignored -> new ArrayList<>())
                    .add(result);
        }

        List<StudentSubjectPerformance> subjectPerformance =
                resultsBySubject.values().stream()
                        .map(this::toSubjectPerformance)
                        .sorted(Comparator.comparing(
                                StudentSubjectPerformance::subjectName,
                                String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(StudentSubjectPerformance::subjectId))
                        .toList();

        return new StudentDashboardResponse(
                testsTaken,
                testsPassed,
                testsFailed,
                passRate,
                subjectPerformance);
    }

    private StudentSubjectPerformance toSubjectPerformance(
            List<ExamResult> subjectResults) {
        ExamResult firstResult = subjectResults.get(0);
        long testsTaken = subjectResults.size();
        long testsPassed = subjectResults.stream()
                .filter(ExamResult::isPassed)
                .count();
        BigDecimal totalPercentage = subjectResults.stream()
                .map(this::scorePercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averagePercentage = totalPercentage.divide(
                BigDecimal.valueOf(testsTaken),
                PERCENTAGE_SCALE,
                ROUNDING_MODE);

        return new StudentSubjectPerformance(
                firstResult.getExam().getSubject().getId(),
                firstResult.getExam().getSubject().getName(),
                testsTaken,
                testsPassed,
                testsTaken - testsPassed,
                averagePercentage);
    }

    private BigDecimal scorePercentage(ExamResult result) {
        if (result.getMaximumMarks().signum() == 0) {
            return BigDecimal.ZERO.setScale(PERCENTAGE_SCALE, ROUNDING_MODE);
        }
        return result.getScore()
                .multiply(BigDecimal.valueOf(100))
                .divide(result.getMaximumMarks(), PERCENTAGE_SCALE, ROUNDING_MODE);
    }

    private BigDecimal percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO.setScale(PERCENTAGE_SCALE, ROUNDING_MODE);
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), PERCENTAGE_SCALE, ROUNDING_MODE);
    }
}
