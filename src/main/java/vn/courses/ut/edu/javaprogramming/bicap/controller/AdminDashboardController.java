package vn.courses.ut.edu.javaprogramming.bicap.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.courses.ut.edu.javaprogramming.bicap.service.AdminDashboardService;

import java.util.Map;

/**
 * Admin dashboard aggregate stats (EPIC-1 / detail-design §4.2 DashboardPage).
 */
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getDashboard(@RequestHeader("X-Actor-Email") String actorEmail) {
        return ResponseEntity.ok(dashboardService.getDashboard(actorEmail));
    }
}
