package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.entity.Driver;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Shipment;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Vehicle;

import java.time.LocalDateTime;

/**
 * Summary response for a single shipment — used in list views.
 * Includes order headline, driver and vehicle identifiers.
 */
public class ShipmentResponse {

    private Long id;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime pickupTime;
    private LocalDateTime deliveryTime;
    private String routeSummary;

    // Order summary
    private Long orderId;
    private String deliveryAddr;

    // Driver info
    private Long driverId;
    private String driverName;
    private String driverPhone;

    // Vehicle info
    private Long vehicleId;
    private String vehicleLicensePlate;
    private String vehicleType;

    public ShipmentResponse() {}

    public static ShipmentResponse from(Shipment s, Order order, Driver driver, User driverUser, Vehicle vehicle) {
        ShipmentResponse r = new ShipmentResponse();
        r.id = s.getId();
        r.status = s.getStatus();
        r.createdAt = s.getCreatedAt();
        r.pickupTime = s.getPickupTime();
        r.deliveryTime = s.getDeliveryTime();
        r.routeSummary = s.getRouteSummary();
        r.orderId = s.getOrderId();
        if (order != null) {
            r.deliveryAddr = order.getDeliveryAddr();
        }
        if (driver != null) {
            r.driverId = driver.getId();
        }
        if (driverUser != null) {
            r.driverName = driverUser.getFullName();
            r.driverPhone = driverUser.getPhone();
        }
        if (vehicle != null) {
            r.vehicleId = vehicle.getId();
            r.vehicleLicensePlate = vehicle.getLicensePlate();
            r.vehicleType = vehicle.getType();
        }
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getPickupTime() { return pickupTime; }
    public void setPickupTime(LocalDateTime pickupTime) { this.pickupTime = pickupTime; }
    public LocalDateTime getDeliveryTime() { return deliveryTime; }
    public void setDeliveryTime(LocalDateTime deliveryTime) { this.deliveryTime = deliveryTime; }
    public String getRouteSummary() { return routeSummary; }
    public void setRouteSummary(String routeSummary) { this.routeSummary = routeSummary; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getDeliveryAddr() { return deliveryAddr; }
    public void setDeliveryAddr(String deliveryAddr) { this.deliveryAddr = deliveryAddr; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getDriverPhone() { return driverPhone; }
    public void setDriverPhone(String driverPhone) { this.driverPhone = driverPhone; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public String getVehicleLicensePlate() { return vehicleLicensePlate; }
    public void setVehicleLicensePlate(String vehicleLicensePlate) { this.vehicleLicensePlate = vehicleLicensePlate; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
}
