package com.examplatform.student.controller;

import com.examplatform.student.dto.StudentResultResponse;
import com.examplatform.student.service.StudentExamService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/results")
public class StudentResultController {

    private final StudentExamService studentExamService;

    public StudentResultController(StudentExamService studentExamService) {
        this.studentExamService = studentExamService;
    }

    @GetMapping("/{resultId}")
    public StudentResultResponse getResult(@PathVariable Long resultId) {
        return studentExamService.getResult(resultId);
    }
}
