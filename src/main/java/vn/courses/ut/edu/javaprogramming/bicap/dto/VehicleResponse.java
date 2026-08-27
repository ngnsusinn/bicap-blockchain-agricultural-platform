package vn.courses.ut.edu.javaprogramming.bicap.dto;

import java.time.LocalDateTime;

/** Thông tin phương tiện vận chuyển. */
public class VehicleResponse {
    private Long id;
    private String licensePlate;
    private String type;
    private Double capacity;
    private String status;
    private LocalDateTime createdAt;

    public VehicleResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Double getCapacity() { return capacity; }
    public void setCapacity(Double capacity) { this.capacity = capacity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
