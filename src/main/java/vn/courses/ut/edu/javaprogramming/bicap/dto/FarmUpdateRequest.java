package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Editable farm registration fields for the authenticated Farm Manager. */
public class FarmUpdateRequest {
    @NotBlank @Size(max = 255) private String name;
    @NotBlank @Size(max = 500) private String address;
    @NotNull @DecimalMin("0.01") private Double area;
    @DecimalMin("-90.0") @DecimalMax("90.0") private Double gpsLat;
    @DecimalMin("-180.0") @DecimalMax("180.0") private Double gpsLng;
    @Size(max = 2000) private String description;
    @NotBlank @Size(max = 500) private String productTypes;

    public String getName() { return name; } public void setName(String value) { name = value; }
    public String getAddress() { return address; } public void setAddress(String value) { address = value; }
    public Double getArea() { return area; } public void setArea(Double value) { area = value; }
    public Double getGpsLat() { return gpsLat; } public void setGpsLat(Double value) { gpsLat = value; }
    public Double getGpsLng() { return gpsLng; } public void setGpsLng(Double value) { gpsLng = value; }
    public String getDescription() { return description; } public void setDescription(String value) { description = value; }
    public String getProductTypes() { return productTypes; } public void setProductTypes(String value) { productTypes = value; }
}
