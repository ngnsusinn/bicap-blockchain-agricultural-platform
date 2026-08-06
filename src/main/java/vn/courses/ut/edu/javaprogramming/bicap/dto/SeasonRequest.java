package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * Request payload for creating a new farming season (BICAP-14 / SRS-FM-008).
 */
public class SeasonRequest {

    @NotBlank(message = "Tên mùa vụ không được để trống")
    @Size(max = 255, message = "Tên mùa vụ không vượt quá 255 ký tự")
    private String name;

    @Size(max = 2000, message = "Mô tả không vượt quá 2000 ký tự")
    private String description;

    @NotBlank(message = "Loại sản phẩm không được để trống")
    @Size(max = 100, message = "Loại sản phẩm không vượt quá 100 ký tự")
    private String productType;

    @NotBlank(message = "Giống cây/vật nuôi không được để trống")
    @Size(max = 100, message = "Giống cây/vật nuôi không vượt quá 100 ký tự")
    private String variety;

    @NotNull(message = "Diện tích không được để trống")
    @DecimalMin(value = "0.01", message = "Diện tích phải lớn hơn 0")
    private Double area;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    private LocalDate endDate;

    @Size(max = 2000, message = "Ghi chú không vượt quá 2000 ký tự")
    private String notes;

    public SeasonRequest() {}

    public SeasonRequest(String name, String productType, String variety,
                         Double area, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.productType = productType;
        this.variety = variety;
        this.area = area;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }
    public Double getArea() { return area; }
    public void setArea(Double area) { this.area = area; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
