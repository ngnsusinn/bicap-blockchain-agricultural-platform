package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ReportCreateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ReportResponse;
import vn.courses.ut.edu.javaprogramming.bicap.service.ReportService;

import java.util.List;
import java.util.Map;

/**
 * Báo cáo gửi Admin (BICAP-27 / SRS-FM-021) — API dùng chung cho mọi vai trò.
 *
 * <p>Người dùng (Farm/Retailer/Shipping/Driver) gửi báo cáo qua {@code POST /api/reports};
 * Admin xem và xử lý qua {@code /api/reports/admin/**}. Quyền được kiểm tra trong service.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** Bất kỳ người dùng đã đăng nhập nào cũng có thể gửi báo cáo cho Admin. */
    @PostMapping
    public ResponseEntity<ReportResponse> createReport(@Valid @RequestBody ReportCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.createReport(request));
    }

    /** Danh sách báo cáo do chính người dùng hiện tại gửi. */
    @GetMapping("/my")
    public ResponseEntity<List<ReportResponse>> getMyReports() {
        return ResponseEntity.ok(reportService.getMyReports());
    }

    // ── ADMIN ─────────────────────────────────────────────────────────────────

    @GetMapping("/admin")
    public ResponseEntity<List<ReportResponse>> getReports(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String reporterRole) {
        return ResponseEntity.ok(reportService.getReports(status, type, reporterRole));
    }

    @GetMapping("/admin/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(reportService.getStats());
    }

    @GetMapping("/admin/{id}")
    public ResponseEntity<ReportResponse> getReport(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getReport(id));
    }

    @PutMapping("/admin/{id}/handle")
    public ResponseEntity<ReportResponse> handleReport(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(reportService.handleReport(id, body.get("status"), body.get("adminResponse")));
    }
}
