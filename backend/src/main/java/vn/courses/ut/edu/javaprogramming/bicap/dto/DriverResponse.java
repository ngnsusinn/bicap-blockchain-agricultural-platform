package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.entity.Driver;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Vehicle;
import java.time.LocalDateTime;

public class DriverResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String userPhone;
    private String citizenId;
    private String licenseNumber;
    private String status;
    private LocalDateTime createdAt;

    // Assigned vehicle info (nullable)
    private Long vehicleId;
    private String vehicleLicensePlate;
    private String vehicleType;

    public DriverResponse() {}

    public static DriverResponse from(Driver d, User user, Vehicle vehicle) {
        DriverResponse r = new DriverResponse();
        r.id = d.getId();
        r.userId = d.getUserId();
        r.citizenId = d.getCitizenId();
        r.licenseNumber = d.getLicenseNumber();
        r.status = d.getStatus();
        r.createdAt = d.getCreatedAt();
        if (user != null) {
            r.userName = user.getFullName();
            r.userEmail = user.getEmail();
            r.userPhone = user.getPhone();
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
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    public String getCitizenId() { return citizenId; }
    public void setCitizenId(String citizenId) { this.citizenId = citizenId; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public String getVehicleLicensePlate() { return vehicleLicensePlate; }
    public void setVehicleLicensePlate(String vehicleLicensePlate) { this.vehicleLicensePlate = vehicleLicensePlate; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
}
