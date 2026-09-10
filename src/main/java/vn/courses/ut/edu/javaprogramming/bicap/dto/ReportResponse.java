package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.entity.Report;

import java.time.LocalDateTime;

/**
 * Response payload cho một báo cáo gửi Admin (BICAP-27).
 */
public class ReportResponse {
    private Long id;
    private Long reporterId;
    private String reporterName;
    private String reporterRole;
    private String type;
    private String subject;
    private String content;
    private Long relatedOrderId;
    private String status;
    private String adminResponse;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ReportResponse() {}

    public static ReportResponse fromEntity(Report r) {
        return fromEntity(r, null);
    }

    public static ReportResponse fromEntity(Report r, String reporterName) {
        ReportResponse d = new ReportResponse();
        d.id = r.getId();
        d.reporterId = r.getReporterId();
        d.reporterName = reporterName;
        d.reporterRole = r.getReporterRole();
        d.type = r.getType();
        d.subject = r.getSubject();
        d.content = r.getContent();
        d.relatedOrderId = r.getRelatedOrderId();
        d.status = r.getStatus();
        d.adminResponse = r.getAdminResponse();
        d.handledAt = r.getHandledAt();
        d.createdAt = r.getCreatedAt();
        d.updatedAt = r.getUpdatedAt();
        return d;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReporterId() { return reporterId; }
    public void setReporterId(Long reporterId) { this.reporterId = reporterId; }
    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    public String getReporterRole() { return reporterRole; }
    public void setReporterRole(String reporterRole) { this.reporterRole = reporterRole; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getRelatedOrderId() { return relatedOrderId; }
    public void setRelatedOrderId(Long relatedOrderId) { this.relatedOrderId = relatedOrderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAdminResponse() { return adminResponse; }
    public void setAdminResponse(String adminResponse) { this.adminResponse = adminResponse; }
    public LocalDateTime getHandledAt() { return handledAt; }
    public void setHandledAt(LocalDateTime handledAt) { this.handledAt = handledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
