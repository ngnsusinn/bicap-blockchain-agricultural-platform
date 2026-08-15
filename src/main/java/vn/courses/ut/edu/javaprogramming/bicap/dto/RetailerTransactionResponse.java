package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.common.util.ImagesJson;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Một giao dịch (đơn hàng) trong lịch sử giao dịch của Nhà bán lẻ với nông trại
 * của Farm Manager (BICAP-21 / SRS-FM-015). Dùng trong chi tiết đối tác.
 */
public class RetailerTransactionResponse {
    private Long orderId;
    private String status;
    private LocalDateTime createdAt;

    private Long productId;
    private String productName;
    private String productImage;

    private Long farmId;
    private String farmName;

    private Double quantity;
    private BigDecimal price;
    private BigDecimal totalAmount;

    public RetailerTransactionResponse() {
    }

    public RetailerTransactionResponse(Long orderId, String status, LocalDateTime createdAt,
                                       Long productId, String productName, String productImage,
                                       Long farmId, String farmName, Double quantity, BigDecimal price,
                                       BigDecimal totalAmount) {
        this.orderId = orderId;
        this.status = status;
        this.createdAt = createdAt;
        this.productId = productId;
        this.productName = productName;
        this.productImage = productImage;
        this.farmId = farmId;
        this.farmName = farmName;
        this.quantity = quantity;
        this.price = price;
        this.totalAmount = totalAmount;
    }

    /** Builds the response from pre-loaded entities (product/season/farm may be null-safe). */
    public static RetailerTransactionResponse from(Order order, Product product, FarmingSeason season, Farm farm) {
        List<String> images = product != null ? ImagesJson.parse(product.getImages()) : List.of();
        BigDecimal totalAmount = order.getPrice() != null && order.getQuantity() != null
                ? order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()))
                : null;
        return new RetailerTransactionResponse(
                order.getId(),
                order.getStatus(),
                order.getCreatedAt(),
                product != null ? product.getId() : null,
                product != null ? product.getName() : null,
                images.isEmpty() ? null : images.get(0),
                farm != null ? farm.getId() : null,
                farm != null ? farm.getName() : null,
                order.getQuantity(),
                order.getPrice(),
                totalAmount
        );
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }
    public String getFarmName() { return farmName; }
    public void setFarmName(String farmName) { this.farmName = farmName; }
    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
