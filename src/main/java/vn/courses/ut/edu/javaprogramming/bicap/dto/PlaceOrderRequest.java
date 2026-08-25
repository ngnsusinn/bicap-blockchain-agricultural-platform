package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body khi Retailer đặt mua nông sản (BICAP-75).
 */
public class PlaceOrderRequest {

    @NotNull(message = "productId is required")
    private Long productId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Double quantity;

    @NotBlank(message = "deliveryAddr is required")
    @Size(max = 500, message = "deliveryAddr must not exceed 500 characters")
    private String deliveryAddr;

    @NotNull(message = "proposedPrice is required")
    @DecimalMin(value = "0.01", message = "proposedPrice must be greater than 0")
    private BigDecimal proposedPrice;

    @NotNull(message = "desiredDeliveryDate is required")
    @Future(message = "desiredDeliveryDate must be in the future")
    private LocalDate desiredDeliveryDate;

    @Size(max = 2000, message = "notes must not exceed 2000 characters")
    private String notes;

    public PlaceOrderRequest() {}

    public PlaceOrderRequest(Long productId, Double quantity, String deliveryAddr) {
        this.productId = productId;
        this.quantity = quantity;
        this.deliveryAddr = deliveryAddr;
    }

    public PlaceOrderRequest(Long productId, Double quantity, String deliveryAddr, BigDecimal proposedPrice,
                             LocalDate desiredDeliveryDate, String notes) {
        this(productId, quantity, deliveryAddr);
        this.proposedPrice = proposedPrice; this.desiredDeliveryDate = desiredDeliveryDate; this.notes = notes;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public String getDeliveryAddr() { return deliveryAddr; }
    public void setDeliveryAddr(String deliveryAddr) { this.deliveryAddr = deliveryAddr; }
    public BigDecimal getProposedPrice() { return proposedPrice; }
    public void setProposedPrice(BigDecimal proposedPrice) { this.proposedPrice = proposedPrice; }
    public LocalDate getDesiredDeliveryDate() { return desiredDeliveryDate; }
    public void setDesiredDeliveryDate(LocalDate desiredDeliveryDate) { this.desiredDeliveryDate = desiredDeliveryDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
