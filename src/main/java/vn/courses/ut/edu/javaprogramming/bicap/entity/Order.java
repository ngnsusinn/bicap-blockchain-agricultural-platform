package vn.courses.ut.edu.javaprogramming.bicap.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_retailer_id", columnList = "retailer_id"),
        @Index(name = "idx_orders_deposit_code", columnList = "deposit_code")
})
public class Order {

    /** Order states used by the deposit/payment workflow (kept as String column, values centralized here). */
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_DEPOSIT_PAID = "DEPOSIT_PAID";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "retailer_id")
    private Long retailerId;

    private Double quantity;
    private BigDecimal price;
    private String status;

    @Column(name = "delivery_addr")
    private String deliveryAddr;

    @Column(name = "deposit_rate")
    private Double depositRate = 0.3;

    /** Transfer memo code generated for the deposit (persisted so the webhook can verify and dedup). */
    @Column(name = "deposit_code", unique = true)
    private String depositCode;

    /** Expected deposit amount (persisted so the webhook can verify the transferred amount). */
    @Column(name = "deposit_amount")
    private BigDecimal depositAmount;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Order() {
    }

    public Order(Long id, Long productId, Long retailerId, Double quantity, BigDecimal price, String status,
                 String deliveryAddr, Double depositRate, String depositCode, BigDecimal depositAmount,
                 LocalDateTime createdAt) {
        this.id = id;
        this.productId = productId;
        this.retailerId = retailerId;
        this.quantity = quantity;
        this.price = price;
        this.status = status;
        this.deliveryAddr = deliveryAddr;
        this.depositRate = depositRate;
        this.depositCode = depositCode;
        this.depositAmount = depositAmount;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.depositRate == null) {
            this.depositRate = 0.3;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getRetailerId() { return retailerId; }
    public void setRetailerId(Long retailerId) { this.retailerId = retailerId; }
    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDeliveryAddr() { return deliveryAddr; }
    public void setDeliveryAddr(String deliveryAddr) { this.deliveryAddr = deliveryAddr; }
    public Double getDepositRate() { return depositRate; }
    public void setDepositRate(Double depositRate) { this.depositRate = depositRate; }
    public String getDepositCode() { return depositCode; }
    public void setDepositCode(String depositCode) { this.depositCode = depositCode; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
