package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.NotNull;

public class CreateDepositRequest {
    @NotNull(message = "Order ID is required")
    private Long orderId;

    public CreateDepositRequest() {}

    public CreateDepositRequest(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
}