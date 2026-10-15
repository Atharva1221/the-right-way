package com.examplatform.student.controller;

import com.examplatform.student.dto.ExamSubmissionRequest;
import com.examplatform.student.dto.ExamSubmissionResult;
import com.examplatform.student.dto.StudentExamListResponse;
import com.examplatform.student.dto.StudentExamResponse;
import com.examplatform.student.service.StudentExamService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/exams")
public class StudentExamController {

    private final StudentExamService studentExamService;

    public StudentExamController(StudentExamService studentExamService) {
        this.studentExamService = studentExamService;
    }

    @GetMapping
    public List<StudentExamListResponse> listAvailableExams() {
        return studentExamService.listAvailableExams();
    }

    @GetMapping("/{examId}")
    public StudentExamResponse getExam(@PathVariable Long examId) {
        return studentExamService.getExam(examId);
    }

    @PostMapping("/{examId}/submissions")
    public ExamSubmissionResult submitAnswers(
            @PathVariable Long examId,
            @Valid @RequestBody ExamSubmissionRequest request) {
        return studentExamService.submitAnswers(examId, request);
    }
}
