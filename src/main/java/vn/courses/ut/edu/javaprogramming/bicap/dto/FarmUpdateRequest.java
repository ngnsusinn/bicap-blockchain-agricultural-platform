package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.*;

/**
 * Request payload for updating farm information (BICAP-9 / SRS-FM-003).
 * Farm Manager cập nhật thông tin nông trại: tên, địa chỉ, diện tích, GPS, loại cây trồng, mô tả.
 * Sau khi cập nhật, trạng thái nông trại sẽ chuyển về PENDING để Admin xét duyệt lại (BR1).
 */
public class FarmUpdateRequest {

    @NotBlank(message = "Tên nông trại không được để trống")
    @Size(max = 255, message = "Tên nông trại không vượt quá 255 ký tự")
    private String name;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 500, message = "Địa chỉ không vượt quá 500 ký tự")
    private String address;

    @NotNull(message = "Diện tích không được để trống")
    @DecimalMin(value = "0.01", message = "Diện tích phải lớn hơn 0")
    private Double area;

    private Double gpsLat;

    private Double gpsLng;

    @Size(max = 2000, message = "Mô tả không vượt quá 2000 ký tự")
    private String description;

    @Size(max = 500, message = "Loại cây trồng/vật nuôi không vượt quá 500 ký tự")
    private String productTypes;

    public FarmUpdateRequest() {}

    public FarmUpdateRequest(String name, String address, Double area, Double gpsLat, Double gpsLng,
                             String description, String productTypes) {
        this.name = name;
        this.address = address;
        this.area = area;
        this.gpsLat = gpsLat;
        this.gpsLng = gpsLng;
        this.description = description;
        this.productTypes = productTypes;
    }

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
