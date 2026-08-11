package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class SeasonCreateRequest {

    @NotBlank(message = "Season name is required")
    @Size(max = 255, message = "Season name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Product type is required")
    @Size(max = 100, message = "Product type must not exceed 100 characters")
    private String productType;

    @NotBlank(message = "Variety is required")
    @Size(max = 100, message = "Variety must not exceed 100 characters")
    private String variety;

    @NotNull(message = "Area is required")
    @DecimalMin(value = "0.01", message = "Area must be positive")
    private Double area;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    public SeasonCreateRequest() {
    }

    public SeasonCreateRequest(String name, String productType, String variety, Double area, LocalDate startDate) {
        this.name = name;
        this.productType = productType;
        this.variety = variety;
        this.area = area;
        this.startDate = startDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getVariety() {
        return variety;
    }

    public void setVariety(String variety) {
        this.variety = variety;
    }

    public Double getArea() {
        return area;
    }

    public void setArea(Double area) {
        this.area = area;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
}
