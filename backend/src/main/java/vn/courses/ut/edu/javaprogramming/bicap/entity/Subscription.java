package vn.courses.ut.edu.javaprogramming.bicap.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "subscriptions",
        indexes = { @Index(name = "idx_subscriptions_farm_id", columnList = "farm_id") })
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "farm_id", nullable = false)
    private Long farmId;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "payment_code")
    private String paymentCode;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status")
    private SubscriptionRequestStatus requestStatus;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Subscription() {
    }

    public Subscription(Long id, Long farmId, Long packageId, String paymentCode, LocalDate startDate,
                        LocalDate endDate, SubscriptionStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.farmId = farmId;
        this.packageId = packageId;
        this.paymentCode = paymentCode;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.createdAt = createdAt;
        this.requestStatus = status == SubscriptionStatus.ACTIVE
            ? SubscriptionRequestStatus.APPROVED : SubscriptionRequestStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = SubscriptionStatus.PENDING_PAYMENT;
        }
        if (this.requestStatus == null) {
            this.requestStatus = SubscriptionRequestStatus.PENDING;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }
    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }
    public String getPaymentCode() { return paymentCode; }
    public void setPaymentCode(String paymentCode) { this.paymentCode = paymentCode; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public SubscriptionStatus getStatus() { return status; }
    public void setStatus(SubscriptionStatus status) { this.status = status; }
    public SubscriptionRequestStatus getRequestStatus() { return requestStatus; }
    public void setRequestStatus(SubscriptionRequestStatus requestStatus) { this.requestStatus = requestStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long farmId;
        private Long packageId;
        private String paymentCode;
        private LocalDate startDate;
        private LocalDate endDate;
        private SubscriptionStatus status;
        private SubscriptionRequestStatus requestStatus;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder farmId(Long farmId) { this.farmId = farmId; return this; }
        public Builder packageId(Long packageId) { this.packageId = packageId; return this; }
        public Builder paymentCode(String paymentCode) { this.paymentCode = paymentCode; return this; }
        public Builder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public Builder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public Builder status(SubscriptionStatus status) { this.status = status; return this; }
        public Builder requestStatus(SubscriptionRequestStatus requestStatus) { this.requestStatus = requestStatus; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Subscription build() {
            Subscription subscription = new Subscription(id, farmId, packageId, paymentCode, startDate, endDate, status, createdAt);
            subscription.requestStatus = requestStatus;
            return subscription;
        }
    }
}
