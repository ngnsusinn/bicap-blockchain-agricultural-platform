package vn.courses.ut.edu.javaprogramming.bicap.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {
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

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Order() {
    }

    public Order(Long id, Long productId, Long retailerId, Double quantity, BigDecimal price, String status, String deliveryAddr, Double depositRate, LocalDateTime createdAt) {
        this.id = id;
        this.productId = productId;
        this.retailerId = retailerId;
        this.quantity = quantity;
        this.price = price;
        this.status = status;
        this.deliveryAddr = deliveryAddr;
        this.depositRate = depositRate;
        this.createdAt = createdAt;
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}