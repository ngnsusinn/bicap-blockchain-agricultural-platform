package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.NotNull;

public class ShipmentCreateRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Driver ID is required")
    private Long driverId;

    @NotNull(message = "Vehicle ID is required")
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
