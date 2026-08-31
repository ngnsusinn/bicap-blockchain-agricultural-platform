package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Season status transition (BICAP-12→16). When moving to HARVESTED the farm must report
 * the harvested amount so the season becomes exportable (BICAP-16 validates against it).
 */
public class SeasonStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "HARVESTED|CANCELLED", message = "Status must be HARVESTED or CANCELLED")
    private String status;

    @DecimalMin(value = "0.01", message = "Harvested quantity must be positive")
    @Digits(integer = 16, fraction = 2, message = "Harvested quantity has too many digits")
    private BigDecimal harvestedQuantity;

    @Size(max = 30, message = "Harvest unit must not exceed 30 characters")
    private String harvestUnit;

    public SeasonStatusUpdateRequest() {
    }

    public SeasonStatusUpdateRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getHarvestedQuantity() {
        return harvestedQuantity;
    }

    public void setHarvestedQuantity(BigDecimal harvestedQuantity) {
        this.harvestedQuantity = harvestedQuantity;
    }

    public String getHarvestUnit() {
        return harvestUnit;
    }

    public void setHarvestUnit(String harvestUnit) {
        this.harvestUnit = harvestUnit;
    }
}
