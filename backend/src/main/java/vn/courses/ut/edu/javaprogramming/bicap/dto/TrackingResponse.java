package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.entity.ShipmentTracking;
import vn.courses.ut.edu.javaprogramming.bicap.common.util.ImagesJson;

import java.time.LocalDateTime;
import java.util.List;

public class TrackingResponse {

    private Long id;
    private Long shipmentId;
    private String status;
    private Double gpsLat;
    private Double gpsLng;
    private List<String> images;
    private String notes;
    private LocalDateTime timestamp;

    public TrackingResponse() {}

    public static TrackingResponse from(ShipmentTracking t) {
        TrackingResponse r = new TrackingResponse();
        r.id = t.getId();
        r.shipmentId = t.getShipmentId();
        r.status = t.getStatus();
        r.gpsLat = t.getGpsLat();
        r.gpsLng = t.getGpsLng();
        r.images = ImagesJson.parse(t.getImages());
        r.notes = t.getNotes();
        r.timestamp = t.getTimestamp();
        return r;
    }

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
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
