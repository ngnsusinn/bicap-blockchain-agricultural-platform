package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Lý do từ chối yêu cầu mua nông sản (BICAP-20 / SRS-FM-014).
 * Bắt buộc khi Farm Manager từ chối đơn hàng PENDING.
 *
 * @param reason Lý do từ chối (không rỗng, tối đa 1000 ký tự)
 */
public record RejectOrderRequest(
        @NotBlank(message = "Rejection reason is required")
        @Size(max = 1000, message = "Rejection reason must not exceed 1000 characters")
        String reason) {
}
