package vn.courses.ut.edu.javaprogramming.bicap.dto;

import java.time.LocalDateTime;

/** Một bản ghi tracking trong quy trình vận chuyển — BICAP-57. */
public class ShipmentTrackingResponse {
    private Long id;
    private Long shipmentId;
    private String status;
    private Double gpsLat;
    private Double gpsLng;
    private String images;
    private String notes;
    private LocalDateTime timestamp;

    public ShipmentTrackingResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getShipmentId() { return shipmentId; }
    public void setShipmentId(Long shipmentId) { this.shipmentId = shipmentId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getGpsLat() { return gpsLat; }
    public void setGpsLat(Double gpsLat) { this.gpsLat = gpsLat; }
    public Double getGpsLng() { return gpsLng; }
    public void setGpsLng(Double gpsLng) { this.gpsLng = gpsLng; }
    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
