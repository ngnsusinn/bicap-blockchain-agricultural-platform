package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Driver confirms pickup at the farm (BICAP-64 / BICAP-76).
 *
 * <p>F11 fix: {@code traceHash} carries the value scanned from the product's traceability
 * QR. The service matches it against the trace hash of the export attached to the ordered
 * product, so a driver cannot confirm pickup of the wrong consignment.
 */
public class PickupConfirmRequest {

    @NotNull(message = "GPS latitude is required")
    private Double gpsLat;

    @NotNull(message = "GPS longitude is required")
    private Double gpsLng;

    /** Value decoded from the traceability QR at the farm (0x + 64 hex). */
    @Size(max = 100, message = "traceHash is too long")
    private String traceHash;

    private List<String> images;
    private String notes;

    public PickupConfirmRequest() {}

    public Double getGpsLat() { return gpsLat; }
    public void setGpsLat(Double gpsLat) { this.gpsLat = gpsLat; }
    public Double getGpsLng() { return gpsLng; }
    public void setGpsLng(Double gpsLng) { this.gpsLng = gpsLng; }
    public String getTraceHash() { return traceHash; }
    public void setTraceHash(String traceHash) { this.traceHash = traceHash; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
