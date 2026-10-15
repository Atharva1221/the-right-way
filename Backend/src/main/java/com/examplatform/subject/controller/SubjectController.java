package com.examplatform.subject.controller;

import com.examplatform.user.enums.UserRole;
import com.examplatform.subject.dto.CreateSubjectRequest;
import com.examplatform.subject.dto.SubjectResponse;
import com.examplatform.subject.dto.UpdateSubjectRequest;
import com.examplatform.subject.service.SubjectService;
import com.examplatform.user.entity.User;
import com.examplatform.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectService subjectService;
    private final UserRepository userRepository;

    public SubjectController(
            SubjectService subjectService,
            UserRepository userRepository) {
        this.subjectService = subjectService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<SubjectResponse> createSubject(
            @Valid @RequestBody CreateSubjectRequest request,
            Authentication authentication) {

        User admin = getAdmin(authentication);

        var subject = subjectService.createSubject(
                admin.getId(),
                request.name(),
                request.description()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SubjectResponse.from(subject));
    }

    @GetMapping
    public ResponseEntity<List<SubjectResponse>> getSubjects(
            Authentication authentication) {

        User admin = getAdmin(authentication);

        List<SubjectResponse> subjects = subjectService
                .getSubjects(admin.getId())
                .stream()
                .map(SubjectResponse::from)
                .toList();

        return ResponseEntity.ok(subjects);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubjectResponse> updateSubject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSubjectRequest request,
            Authentication authentication) {

        User admin = getAdmin(authentication);

        var subject = subjectService.updateSubject(
                admin.getId(),
                id,
                request.name(),
                request.description()
        );

        return ResponseEntity.ok(SubjectResponse.from(subject));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubject(
            @PathVariable Long id,
            Authentication authentication) {

        User admin = getAdmin(authentication);

        subjectService.deleteSubject(admin.getId(), id);

        return ResponseEntity.noContent().build();
    }

    private User getAdmin(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if (user.getRole() != UserRole.ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only admins can manage subjects");
        }

        return user;
    }
}