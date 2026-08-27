package vn.courses.ut.edu.javaprogramming.bicap.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Tài xế vận chuyển trong hệ thống BICAP.
 * Bảng: drivers — BICAP-55/56/57
 */
@Entity
@Table(name = "drivers")
public class Driver {

    public static final String STATUS_IDLE     = "IDLE";
    public static final String STATUS_ON_TRIP  = "ON_TRIP";
    public static final String STATUS_OFFLINE  = "OFFLINE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → users(id) — user account with SHIP_DRIVER role */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "citizen_id", nullable = false, unique = true, length = 20)
    private String citizenId;

    @Column(name = "license_number", nullable = false, unique = true, length = 30)
    private String licenseNumber;

    /** FK → vehicles(id) ON DELETE SET NULL */
    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(nullable = false, length = 20)
    private String status = STATUS_IDLE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Driver() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = STATUS_IDLE;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getCitizenId() { return citizenId; }
    public void setCitizenId(String citizenId) { this.citizenId = citizenId; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
