package vn.courses.ut.edu.javaprogramming.bicap.dto;

public class DriverUpdateRequest {

    private String citizenId;
    private String licenseNumber;
    private Long vehicleId;
    private String status;

    public DriverUpdateRequest() {}

    public String getCitizenId() { return citizenId; }
    public void setCitizenId(String citizenId) { this.citizenId = citizenId; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
