package vn.courses.ut.edu.javaprogramming.bicap.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Chi tiết lô vận chuyển, bao gồm thông tin đơn hàng, tài xế, phương tiện
 * và lịch sử tracking (BICAP-57).
 */
public class ShipmentResponse {
    private Long id;
    private String status;
    private LocalDateTime pickupTime;
    private LocalDateTime deliveryTime;
    private String routeSummary;
    private LocalDateTime createdAt;

    // Order info
    private Long orderId;
    private String orderStatus;
    private String deliveryAddr;
    private BigDecimal totalAmount;

    // Product info (via order)
    private String productName;
    private Double quantity;
    private String retailerName;
    private String retailerEmail;

    // Driver info
    private Long driverId;
    private String driverName;
    private String driverPhone;
    private String driverLicenseNumber;
    private String driverCitizenId;

    // Vehicle info
    private Long vehicleId;
    private String vehicleLicensePlate;
    private String vehicleType;
    private Double vehicleCapacity;

    // Tracking history (for detail — BICAP-57)
    private List<ShipmentTrackingResponse> trackingHistory;

    public ShipmentResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getPickupTime() { return pickupTime; }
    public void setPickupTime(LocalDateTime pickupTime) { this.pickupTime = pickupTime; }
    public LocalDateTime getDeliveryTime() { return deliveryTime; }
    public void setDeliveryTime(LocalDateTime deliveryTime) { this.deliveryTime = deliveryTime; }
    public String getRouteSummary() { return routeSummary; }
    public void setRouteSummary(String routeSummary) { this.routeSummary = routeSummary; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    public String getDeliveryAddr() { return deliveryAddr; }
    public void setDeliveryAddr(String deliveryAddr) { this.deliveryAddr = deliveryAddr; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public String getRetailerName() { return retailerName; }
    public void setRetailerName(String retailerName) { this.retailerName = retailerName; }
    public String getRetailerEmail() { return retailerEmail; }
    public void setRetailerEmail(String retailerEmail) { this.retailerEmail = retailerEmail; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getDriverPhone() { return driverPhone; }
    public void setDriverPhone(String driverPhone) { this.driverPhone = driverPhone; }
    public String getDriverLicenseNumber() { return driverLicenseNumber; }
    public void setDriverLicenseNumber(String driverLicenseNumber) { this.driverLicenseNumber = driverLicenseNumber; }
    public String getDriverCitizenId() { return driverCitizenId; }
    public void setDriverCitizenId(String driverCitizenId) { this.driverCitizenId = driverCitizenId; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public String getVehicleLicensePlate() { return vehicleLicensePlate; }
    public void setVehicleLicensePlate(String vehicleLicensePlate) { this.vehicleLicensePlate = vehicleLicensePlate; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public Double getVehicleCapacity() { return vehicleCapacity; }
    public void setVehicleCapacity(Double vehicleCapacity) { this.vehicleCapacity = vehicleCapacity; }
    public List<ShipmentTrackingResponse> getTrackingHistory() { return trackingHistory; }
    public void setTrackingHistory(List<ShipmentTrackingResponse> trackingHistory) { this.trackingHistory = trackingHistory; }
}
