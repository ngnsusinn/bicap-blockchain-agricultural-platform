package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.*;

/**
 * Payload cập nhật thông tin nông trại của Farm Manager (BICAP-9 / SRS-FM-003).
 * Cho phép thay đổi tên, địa chỉ, diện tích, GPS, mô tả và loại nông sản.
 */
public class FarmUpdateRequest {
    @NotBlank(message = "Farm name is required")
    @Size(max = 255, message = "Farm name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Address is required")
    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @NotNull(message = "Area is required")
    @DecimalMin(value = "0.01", message = "Area must be positive")
    private Double area;

    @Min(value = -90, message = "Latitude must be between -90 and 90")
    @Max(value = 90, message = "Latitude must be between -90 and 90")
    private Double gpsLat;

    @Min(value = -180, message = "Longitude must be between -180 and 180")
    @Max(value = 180, message = "Longitude must be between -180 and 180")
    private Double gpsLng;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Size(max = 500, message = "Product types must not exceed 500 characters")
    private String productTypes;

    public FarmUpdateRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getArea() { return area; }
    public void setArea(Double area) { this.area = area; }
    public Double getGpsLat() { return gpsLat; }
    public void setGpsLat(Double gpsLat) { this.gpsLat = gpsLat; }
    public Double getGpsLng() { return gpsLng; }
    public void setGpsLng(Double gpsLng) { this.gpsLng = gpsLng; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getProductTypes() { return productTypes; }
    public void setProductTypes(String productTypes) { this.productTypes = productTypes; }
}
