package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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

    public PlaceOrderRequest() {}

    public PlaceOrderRequest(Long productId, Double quantity, String deliveryAddr) {
        this.productId = productId;
        this.quantity = quantity;
        this.deliveryAddr = deliveryAddr;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public String getDeliveryAddr() { return deliveryAddr; }
    public void setDeliveryAddr(String deliveryAddr) { this.deliveryAddr = deliveryAddr; }
}
