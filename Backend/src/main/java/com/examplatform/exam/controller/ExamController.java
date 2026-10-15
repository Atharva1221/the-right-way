package com.examplatform.exam.controller;

import com.examplatform.exam.dto.CreateExamRequest;
import com.examplatform.exam.dto.ExamResponse;
import com.examplatform.exam.dto.UpdateExamRequest;

import com.examplatform.exam.service.ExamService;
import com.examplatform.user.entity.User;
import com.examplatform.user.repository.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;
    private final UserRepository userRepository;

    public ExamController(
            ExamService examService,
            UserRepository userRepository) {
        this.examService = examService;
        this.userRepository = userRepository;
    }

    private Long getAdminId(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!"ADMIN".equals(user.getRole().name())) {
            throw new AccessDeniedException("Only admins can manage exams");
        }

        return user.getId();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExamResponse createExam(
            Authentication authentication,
            @Valid @RequestBody CreateExamRequest request) {
        return ExamResponse.from(
                examService.createExam(getAdminId(authentication), request));
    }

    @GetMapping
    public List<ExamResponse> getExams(Authentication authentication) {
        return examService.getExams(getAdminId(authentication))
                .stream()
                .map(ExamResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ExamResponse getExam(
            Authentication authentication,
            @PathVariable Long id) {
        return ExamResponse.from(
                examService.getExam(getAdminId(authentication), id));
    }

    @PutMapping("/{id}")
    public ExamResponse updateExam(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateExamRequest request) {
        return ExamResponse.from(
                examService.updateExam(getAdminId(authentication), id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExam(
            Authentication authentication,
            @PathVariable Long id) {
        examService.deleteExam(getAdminId(authentication), id);
    }

    @PostMapping("/{id}/publish")
    public ExamResponse publishExam(
            Authentication authentication,
            @PathVariable Long id) {
        return ExamResponse.from(
                examService.publishExam(getAdminId(authentication), id));
    }

    @PostMapping("/{id}/archive")
    public ExamResponse archiveExam(
            Authentication authentication,
            @PathVariable Long id) {
        return ExamResponse.from(
                examService.archiveExam(getAdminId(authentication), id));
    }
}