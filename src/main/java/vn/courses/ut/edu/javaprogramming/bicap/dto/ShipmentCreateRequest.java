package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body để tạo lô vận chuyển — BICAP-55.
 * Business rules BR2: driver phải IDLE, BR3: vehicle phải AVAILABLE.
 */
public class ShipmentCreateRequest {

    @NotNull(message = "orderId là bắt buộc")
    private Long orderId;

    @NotNull(message = "driverId là bắt buộc")
    private Long driverId;

    @NotNull(message = "vehicleId là bắt buộc")
    private Long vehicleId;

    private String routeSummary;

    public ShipmentCreateRequest() {}

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public String getRouteSummary() { return routeSummary; }
    public void setRouteSummary(String routeSummary) { this.routeSummary = routeSummary; }
}
