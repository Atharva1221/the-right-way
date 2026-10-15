package com.examplatform.question.controller;

import com.examplatform.question.dto.CreateQuestionRequest;
import com.examplatform.question.dto.QuestionResponse;
import com.examplatform.question.dto.UpdateQuestionRequest;
import com.examplatform.question.service.QuestionService;
import com.examplatform.user.enums.UserRole;
import com.examplatform.user.entity.User;
import com.examplatform.user.repository.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exams/{examId}/questions")
public class QuestionController {

    private final QuestionService questionService;
    private final UserRepository userRepository;

    public QuestionController(
            QuestionService questionService,
            UserRepository userRepository) {
        this.questionService = questionService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse createQuestion(
            Authentication authentication,
            @PathVariable Long examId,
            @Valid @RequestBody CreateQuestionRequest request) {
        return QuestionResponse.from(questionService.createQuestion(
                getAdminId(authentication), examId, request));
    }

    @GetMapping
    public List<QuestionResponse> getQuestions(
            Authentication authentication,
            @PathVariable Long examId) {
        return questionService.getQuestions(getAdminId(authentication), examId)
                .stream()
                .map(QuestionResponse::from)
                .toList();
    }

    @GetMapping("/{questionId}")
    public QuestionResponse getQuestion(
            Authentication authentication,
            @PathVariable Long examId,
            @PathVariable Long questionId) {
        return QuestionResponse.from(questionService.getQuestion(
                getAdminId(authentication), examId, questionId));
    }

    @PutMapping("/{questionId}")
    public QuestionResponse updateQuestion(
            Authentication authentication,
            @PathVariable Long examId,
            @PathVariable Long questionId,
            @Valid @RequestBody UpdateQuestionRequest request) {
        return QuestionResponse.from(questionService.updateQuestion(
                getAdminId(authentication), examId, questionId, request));
    }

    @DeleteMapping("/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestion(
            Authentication authentication,
            @PathVariable Long examId,
            @PathVariable Long questionId) {
        questionService.deleteQuestion(
                getAdminId(authentication), examId, questionId);
    }

    private Long getAdminId(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException(
                    "Only admins can manage exam questions");
        }
        return user.getId();
    }
}
