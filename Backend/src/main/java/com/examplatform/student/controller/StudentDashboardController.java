package com.examplatform.student.controller;

import com.examplatform.student.dto.StudentDashboardResponse;
import com.examplatform.student.service.StudentDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/dashboard")
public class StudentDashboardController {

    private final StudentDashboardService dashboardService;

    public StudentDashboardController(StudentDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public StudentDashboardResponse getDashboard() {
        return dashboardService.getDashboard();
    }
}
