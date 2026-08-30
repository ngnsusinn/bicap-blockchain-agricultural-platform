package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ReportCreateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ReportResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Report;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ReportRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Báo cáo gửi Admin (BICAP-27 / SRS-FM-021).
 *
 * <p>API dùng chung cho mọi vai trò người dùng (Farm Manager, Retailer BICAP-53,
 * Shipping Manager BICAP-60, Driver BICAP-68): người dùng tạo báo cáo, Admin xem và
 * xử lý (resolve/reject). Khi tạo báo cáo, hệ thống gửi thông báo tới các tài khoản
 * Admin; khi Admin phản hồi, người gửi nhận được thông báo.
 */
@Service
@Transactional
@SuppressWarnings("null")
public class ReportService {

    private static final Set<String> REPORT_TYPES = Set.of(
            Report.TYPE_COMPLAINT, Report.TYPE_FEEDBACK, Report.TYPE_INCIDENT, Report.TYPE_OTHER);

    private static final Set<String> ADMIN_ROLES = Set.of("SUPER_ADMIN", "ADMIN");
    private static final Set<String> ADMIN_VIEW_ROLES = Set.of("SUPER_ADMIN", "ADMIN", "MODERATOR");

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ReportService(ReportRepository reportRepository,
                         UserRepository userRepository,
                         NotificationService notificationService) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /** Người dùng bất kỳ (đã đăng nhập) tạo báo cáo gửi Admin. */
    public ReportResponse createReport(ReportCreateRequest request) {
        User actor = CurrentUser.get();

        String type = normalizeType(request.type());
        String reporterRole = actor.getRoles().stream()
                .map(r -> r.getName())
                .findFirst()
                .orElse("USER");

        Report report = new Report();
        report.setReporterId(actor.getId());
        report.setReporterRole(reporterRole);
        report.setType(type);
        report.setSubject(request.subject().trim());
        report.setContent(request.content().trim());
        report.setRelatedOrderId(request.relatedOrderId());
        report.setStatus(Report.STATUS_OPEN);
        Report saved = reportRepository.save(report);

        notifyAdmins(saved);
        return ReportResponse.fromEntity(saved, actor.getFullName());
    }

    /** Danh sách báo cáo do chính người dùng hiện tại gửi. */
    @Transactional(readOnly = true)
    public List<ReportResponse> getMyReports() {
        User actor = CurrentUser.get();
        return reportRepository.findByReporterIdOrderByCreatedAtDesc(actor.getId()).stream()
                .map(r -> ReportResponse.fromEntity(r, actor.getFullName()))
                .toList();
    }

    /** Admin xem danh sách báo cáo (lọc theo trạng thái / loại / vai trò người gửi). */
    @Transactional(readOnly = true)
    public List<ReportResponse> getReports(String status, String type, String reporterRole) {
        requireAdminView();
        return reportRepository.findFiltered(
                normalizeParam(status), normalizeParam(type), normalizeParam(reporterRole)).stream()
                .map(this::toResponseWithName)
                .toList();
    }

    /** Admin xem chi tiết một báo cáo. */
    @Transactional(readOnly = true)
    public ReportResponse getReport(Long id) {
        requireAdminView();
        return toResponseWithName(findReport(id));
    }

    /** Thống kê nhanh số báo cáo theo trạng thái cho dashboard Admin. */
    @Transactional(readOnly = true)
    public java.util.Map<String, Long> getStats() {
        requireAdminView();
        java.util.Map<String, Long> stats = new java.util.LinkedHashMap<>();
        stats.put("open", reportRepository.countByStatus(Report.STATUS_OPEN));
        stats.put("inProgress", reportRepository.countByStatus(Report.STATUS_IN_PROGRESS));
        stats.put("resolved", reportRepository.countByStatus(Report.STATUS_RESOLVED));
        stats.put("rejected", reportRepository.countByStatus(Report.STATUS_REJECTED));
        stats.put("total", reportRepository.count());
        return stats;
    }

    /**
     * Admin phản hồi và chuyển trạng thái báo cáo (RESOLVED / REJECTED / IN_PROGRESS).
     * Người gửi nhận thông báo kết quả.
     */
    public ReportResponse handleReport(Long id, String status, String adminResponse) {
        User admin = requireAdminWrite();
        Report report = findReport(id);

        String newStatus = normalizeStatus(status);
        if (adminResponse == null || adminResponse.trim().isEmpty()) {
            throw new BadRequestException("Admin response is required when handling a report");
        }

        report.setStatus(newStatus);
        report.setAdminResponse(adminResponse.trim());
        report.setHandledById(admin.getId());
        report.setHandledAt(LocalDateTime.now());
        Report saved = reportRepository.save(report);

        String title = "Báo cáo \"" + saved.getSubject() + "\" đã được xử lý";
        String content = "Quản trị viên đã phản hồi báo cáo của bạn (trạng thái: " + newStatus + "): "
                + saved.getAdminResponse();
        notificationService.sendNotification(saved.getReporterId(), "INFO", title, content, false);

        return toResponseWithName(saved);
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private ReportResponse toResponseWithName(Report r) {
        String reporterName = r.getReporterId() != null
                ? userRepository.findById(r.getReporterId()).map(User::getFullName).orElse(null)
                : null;
        return ReportResponse.fromEntity(r, reporterName);
    }

    private Report findReport(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + id));
    }

    private void notifyAdmins(Report report) {
        List<User> admins = userRepository.findDistinctByRoles_NameIn(ADMIN_ROLES);
        String content = report.getReporterRole() + " vừa gửi báo cáo: " + report.getSubject();
        for (User admin : admins) {
            notificationService.sendNotification(admin.getId(), "WARNING",
                    "Báo cáo mới từ người dùng", content, false);
        }
    }

    private String normalizeType(String type) {
        String t = type == null ? "" : type.trim().toUpperCase();
        if (!REPORT_TYPES.contains(t)) {
            throw new BadRequestException("Invalid report type: " + type
                    + " (allowed: COMPLAINT, FEEDBACK, INCIDENT, OTHER)");
        }
        return t;
    }

    private String normalizeStatus(String status) {
        String s = status == null ? "" : status.trim().toUpperCase();
        if (!Set.of(Report.STATUS_OPEN, Report.STATUS_IN_PROGRESS,
                Report.STATUS_RESOLVED, Report.STATUS_REJECTED).contains(s)) {
            throw new BadRequestException("Invalid status: " + status);
        }
        return s;
    }

    private static String normalizeParam(String s) {
        return (s == null || s.isBlank()) ? null : s.trim().toUpperCase();
    }

    private User requireAdminView() {
        User actor = CurrentUser.get();
        ActorAuthorizer.requireRoles(actor, ADMIN_VIEW_ROLES);
        return actor;
    }

    private User requireAdminWrite() {
        User actor = CurrentUser.get();
        ActorAuthorizer.requireRoles(actor, ADMIN_ROLES);
        return actor;
    }
}
