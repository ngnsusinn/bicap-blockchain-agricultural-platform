package vn.courses.ut.edu.javaprogramming.bicap.dto;

import java.math.BigDecimal;

public class PurchasePackageResponse {
    private final Long subscriptionId;
    private final String paymentCode;
    private final String bankName;
    private final String accountNumber;
    private final BigDecimal amount;
    private final String transferContent;

    public PurchasePackageResponse(Long subscriptionId, String paymentCode, String bankName, String accountNumber, BigDecimal amount, String transferContent) {
        this.subscriptionId = subscriptionId;
        this.paymentCode = paymentCode;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.transferContent = transferContent;
    }

    public Long getSubscriptionId() { return subscriptionId; }
    public String getPaymentCode() { return paymentCode; }
    public String getBankName() { return bankName; }
    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getAmount() { return amount; }
    public String getTransferContent() { return transferContent; }
}
