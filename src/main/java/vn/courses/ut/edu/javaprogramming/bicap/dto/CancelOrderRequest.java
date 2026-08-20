package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.Size;

/**
 * Request body khi Retailer hủy đơn hàng (BICAP-75).
 * Lý do hủy là tuỳ chọn nhưng nếu có thì tối đa 1000 ký tự.
 */
public class CancelOrderRequest {

    @Size(max = 1000, message = "reason must not exceed 1000 characters")
    private String reason;

    public CancelOrderRequest() {}

    public CancelOrderRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
