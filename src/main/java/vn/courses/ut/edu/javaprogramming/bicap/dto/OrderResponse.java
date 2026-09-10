package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.common.util.ImagesJson;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Product;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

/**
 * Yêu cầu mua nông sản hiển thị cho Farm Manager (BICAP-20 / SRS-FM-014).
 *
 * <p>Tổng hợp thông tin Retailer (người đặt), sản phẩm &amp; nguồn nông trại (qua
 * lô xuất kho/mùa vụ), số lượng, đơn giá snapshot và thông tin đặt cọc. Dùng cho cả
 * danh sách lẫn chi tiết đơn hàng.
 */
public class OrderResponse {
    private Long id;
    private String status;
    private String rejectReason;
    private LocalDateTime createdAt;

    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal productPrice;
    private Double productQuantity;

    private Long retailerId;
    private String retailerName;
    private String retailerEmail;
    private String retailerPhone;

    private Long farmId;
    private String farmName;
    private Long seasonId;
    private String seasonName;

    private Double quantity;
    private BigDecimal price;
    private BigDecimal totalAmount;
    private String deliveryAddr;
    private Double depositRate;
    private BigDecimal depositAmount;
    private String depositCode;

    // BICAP-75: extended lifecycle fields
    private String cancelledReason;
    private LocalDateTime cancelRequestedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime completedAt;
    private LocalDate desiredDeliveryDate;
    private String notes;
    private LocalDateTime acceptedAt;
    private LocalDateTime depositExpiresAt;

    public OrderResponse() {}

    public OrderResponse(Long id, String status, String rejectReason, LocalDateTime createdAt,
                         Long productId, String productName, String productImage, BigDecimal productPrice,
                         Double productQuantity, Long retailerId, String retailerName, String retailerEmail,
                         String retailerPhone, Long farmId, String farmName, Long seasonId, String seasonName,
                         Double quantity, BigDecimal price, BigDecimal totalAmount, String deliveryAddr,
                         Double depositRate, BigDecimal depositAmount, String depositCode,
                         String cancelledReason, LocalDateTime deliveredAt, LocalDateTime completedAt) {
        this.id = id;
        this.status = status;
        this.rejectReason = rejectReason;
        this.createdAt = createdAt;
        this.productId = productId;
        this.productName = productName;
        this.productImage = productImage;
        this.productPrice = productPrice;
        this.productQuantity = productQuantity;
        this.retailerId = retailerId;
        this.retailerName = retailerName;
        this.retailerEmail = retailerEmail;
        this.retailerPhone = retailerPhone;
        this.farmId = farmId;
        this.farmName = farmName;
        this.seasonId = seasonId;
        this.seasonName = seasonName;
        this.quantity = quantity;
        this.price = price;
        this.totalAmount = totalAmount;
        this.deliveryAddr = deliveryAddr;
        this.depositRate = depositRate;
        this.depositAmount = depositAmount;
        this.depositCode = depositCode;
        this.cancelledReason = cancelledReason;
        this.deliveredAt = deliveredAt;
        this.completedAt = completedAt;
    }

    /** Builds the response from pre-loaded entities (product/season/farm/retailer may be null-safe). */
    public static OrderResponse from(Order order, Product product, FarmingSeason season, Farm farm, User retailer) {
        List<String> images = product != null ? ImagesJson.parse(product.getImages()) : List.of();
        BigDecimal totalAmount = order.getPrice() != null && order.getQuantity() != null
                ? order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()))
                : null;
        OrderResponse response = new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getRejectReason(),
                order.getCreatedAt(),
                product != null ? product.getId() : null,
                product != null ? product.getName() : null,
                images.isEmpty() ? null : images.get(0),
                product != null ? product.getPrice() : null,
                product != null ? product.getQuantity() : null,
                order.getRetailerId(),
                retailer != null ? retailer.getFullName() : null,
                retailer != null ? retailer.getEmail() : null,
                retailer != null ? retailer.getPhone() : null,
                farm != null ? farm.getId() : null,
                farm != null ? farm.getName() : null,
                season != null ? season.getId() : null,
                season != null ? season.getName() : null,
                order.getQuantity(),
                order.getPrice(),
                totalAmount,
                order.getDeliveryAddr(),
                order.getDepositRate(),
                order.getDepositAmount(),
                order.getDepositCode(),
                order.getCancelledReason(),
                order.getDeliveredAt(),
                order.getCompletedAt()
        );
        response.desiredDeliveryDate = order.getDesiredDeliveryDate();
        response.cancelRequestedAt = order.getCancelRequestedAt();
        response.notes = order.getNotes();
        response.acceptedAt = order.getAcceptedAt();
        response.depositExpiresAt = order.getAcceptedAt() == null ? null : order.getAcceptedAt().plusHours(24);
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public BigDecimal getProductPrice() { return productPrice; }
    public void setProductPrice(BigDecimal productPrice) { this.productPrice = productPrice; }
    public Double getProductQuantity() { return productQuantity; }
    public void setProductQuantity(Double productQuantity) { this.productQuantity = productQuantity; }
    public Long getRetailerId() { return retailerId; }
    public void setRetailerId(Long retailerId) { this.retailerId = retailerId; }
    public String getRetailerName() { return retailerName; }
    public void setRetailerName(String retailerName) { this.retailerName = retailerName; }
    public String getRetailerEmail() { return retailerEmail; }
    public void setRetailerEmail(String retailerEmail) { this.retailerEmail = retailerEmail; }
    public String getRetailerPhone() { return retailerPhone; }
    public void setRetailerPhone(String retailerPhone) { this.retailerPhone = retailerPhone; }
    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }
    public String getFarmName() { return farmName; }
    public void setFarmName(String farmName) { this.farmName = farmName; }
    public Long getSeasonId() { return seasonId; }
    public void setSeasonId(Long seasonId) { this.seasonId = seasonId; }
    public String getSeasonName() { return seasonName; }
    public void setSeasonName(String seasonName) { this.seasonName = seasonName; }
    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getDeliveryAddr() { return deliveryAddr; }
    public void setDeliveryAddr(String deliveryAddr) { this.deliveryAddr = deliveryAddr; }
    public Double getDepositRate() { return depositRate; }
    public void setDepositRate(Double depositRate) { this.depositRate = depositRate; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
    public String getDepositCode() { return depositCode; }
    public void setDepositCode(String depositCode) { this.depositCode = depositCode; }
    public String getCancelledReason() { return cancelledReason; }
    public void setCancelledReason(String cancelledReason) { this.cancelledReason = cancelledReason; }
    public LocalDateTime getCancelRequestedAt() { return cancelRequestedAt; }
    public void setCancelRequestedAt(LocalDateTime cancelRequestedAt) { this.cancelRequestedAt = cancelRequestedAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDate getDesiredDeliveryDate() { return desiredDeliveryDate; }
    public void setDesiredDeliveryDate(LocalDate desiredDeliveryDate) { this.desiredDeliveryDate = desiredDeliveryDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    public LocalDateTime getDepositExpiresAt() { return depositExpiresAt; }
    public void setDepositExpiresAt(LocalDateTime depositExpiresAt) { this.depositExpiresAt = depositExpiresAt; }
}
