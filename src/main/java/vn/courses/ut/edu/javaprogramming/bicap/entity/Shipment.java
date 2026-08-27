package vn.courses.ut.edu.javaprogramming.bicap.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Lô vận chuyển trong hệ thống BICAP.
 * Bảng: shipments — BICAP-55/56/57
 *
 * State machine: PICKING_UP → IN_TRANSIT → DELIVERED
 *                                        ↘ RETURNED
 */
@Entity
@Table(name = "shipments", indexes = {
        @Index(name = "idx_shipments_driver_status", columnList = "driver_id,status")
})
public class Shipment {

    public static final String STATUS_PICKING_UP = "PICKING_UP";
    public static final String STATUS_IN_TRANSIT = "IN_TRANSIT";
    public static final String STATUS_DELIVERED  = "DELIVERED";
    public static final String STATUS_RETURNED   = "RETURNED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → orders(id) ON DELETE CASCADE — one shipment per order */
    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    /** FK → drivers(id) ON DELETE SET NULL */
    @Column(name = "driver_id")
    private Long driverId;

    /** FK → vehicles(id) ON DELETE SET NULL */
    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(nullable = false, length = 20)
    private String status = STATUS_PICKING_UP;

    @Column(name = "pickup_time")
    private LocalDateTime pickupTime;

    @Column(name = "delivery_time")
    private LocalDateTime deliveryTime;

    @Column(name = "route_summary", length = 500)
    private String routeSummary;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Shipment() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = STATUS_PICKING_UP;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
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
}
