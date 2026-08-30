package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload gửi báo cáo (khiếu nại / phản hồi / sự cố) lên Admin
 * (BICAP-27 / SRS-FM-021). Các vai trò khác (Retailer BICAP-53, Shipping
 * BICAP-60, Driver BICAP-68) dùng chung API này.
 *
 * @param type          COMPLAINT, FEEDBACK, INCIDENT hoặc OTHER
 * @param subject       Tiêu đề báo cáo (không rỗng, tối đa 200 ký tự)
 * @param content       Nội dung chi tiết (tối thiểu 10, tối đa 4000 ký tự)
 * @param relatedOrderId Mã đơn hàng liên quan (tuỳ chọn)
 */
public record ReportCreateRequest(
        @NotBlank(message = "Report type is required")
        @Size(max = 20, message = "Report type must not exceed 20 characters")
        String type,

        @NotBlank(message = "Subject is required")
        @Size(max = 200, message = "Subject must not exceed 200 characters")
        String subject,

        @NotBlank(message = "Content is required")
        @Size(min = 10, max = 4000, message = "Content must be between 10 and 4000 characters")
        String content,

        Long relatedOrderId) {}
