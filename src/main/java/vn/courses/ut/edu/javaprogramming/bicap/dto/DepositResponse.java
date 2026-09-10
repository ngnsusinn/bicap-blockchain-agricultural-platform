package vn.courses.ut.edu.javaprogramming.bicap.dto;

import java.math.BigDecimal;

public class DepositResponse {
    private Long orderId;
    private String paymentCode;
    private String bankName;
    private String accountNumber;
    private BigDecimal depositAmount;
    private String qrCodeUrl;

    public DepositResponse() {}

    public DepositResponse(Long orderId, String paymentCode, String bankName, String accountNumber, BigDecimal depositAmount, String qrCodeUrl) {
        this.orderId = orderId;
        this.paymentCode = paymentCode;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.depositAmount = depositAmount;
        this.qrCodeUrl = qrCodeUrl;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getPaymentCode() { return paymentCode; }
    public void setPaymentCode(String paymentCode) { this.paymentCode = paymentCode; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
    public String getQrCodeUrl() { return qrCodeUrl; }
    public void setQrCodeUrl(String qrCodeUrl) { this.qrCodeUrl = qrCodeUrl; }
}