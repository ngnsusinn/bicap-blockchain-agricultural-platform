package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Báo cáo sự cố / tình hình vận chuyển từ tài xế gửi lên (BICAP-76).
 */
public class DriverReportRequest {

    @NotNull(message = "Shipment ID is required")
    private Long shipmentId;

    @NotBlank(message = "Report type is required")
    private String reportType; // INCIDENT, DELAY, DAMAGE, OTHER

    @NotBlank(message = "Description is required")
    private String description;

    /** GPS vị trí tại thời điểm gửi báo cáo (optional). */
    private Double gpsLat;
    private Double gpsLng;

    public DriverReportRequest() {}

    public Long getShipmentId() { return shipmentId; }
    public void setShipmentId(Long shipmentId) { this.shipmentId = shipmentId; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getGpsLat() { return gpsLat; }
    public void setGpsLat(Double gpsLat) { this.gpsLat = gpsLat; }
    public Double getGpsLng() { return gpsLng; }
    public void setGpsLng(Double gpsLng) { this.gpsLng = gpsLng; }
}
