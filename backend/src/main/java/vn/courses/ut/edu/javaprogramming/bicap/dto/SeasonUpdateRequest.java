package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class SeasonUpdateRequest {

    @Size(max = 255, message = "Season name must not exceed 255 characters")
    private String name;

    @Size(max = 100, message = "Product type must not exceed 100 characters")
    private String productType;

    @Size(max = 100, message = "Variety must not exceed 100 characters")
    private String variety;

    private Double area;

    private LocalDate startDate;

    private LocalDate endDate;

    public SeasonUpdateRequest() {
    }

    public SeasonUpdateRequest(String name, String productType, String variety, Double area, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.productType = productType;
        this.variety = variety;
        this.area = area;
        this.startDate = startDate;
        this.endDate = endDate;
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

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
