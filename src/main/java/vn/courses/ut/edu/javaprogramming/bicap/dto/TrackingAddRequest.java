package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class TrackingAddRequest {

    @NotBlank(message = "Status is required")
    private String status;

    @NotNull(message = "GPS latitude is required")
    private Double gpsLat;

    @NotNull(message = "GPS longitude is required")
    private Double gpsLng;

    /** Optional list of image URLs taken at this checkpoint. */
    private List<String> images;

    private String notes;

    public TrackingAddRequest() {}

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
}
