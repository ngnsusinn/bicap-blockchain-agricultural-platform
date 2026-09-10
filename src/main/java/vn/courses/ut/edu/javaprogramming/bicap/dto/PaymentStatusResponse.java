package vn.courses.ut.edu.javaprogramming.bicap.dto;

public class PaymentStatusResponse {
    private final String paymentCode;
    private final String status;
    private final Long subscriptionId;
    private final String message;

    public PaymentStatusResponse(String paymentCode, String status, Long subscriptionId, String message) {
        this.paymentCode = paymentCode;
        this.status = status;
        this.subscriptionId = subscriptionId;
        this.message = message;
    }

    public String getPaymentCode() { return paymentCode; }
    public String getStatus() { return status; }
    public Long getSubscriptionId() { return subscriptionId; }
    public String getMessage() { return message; }
}
