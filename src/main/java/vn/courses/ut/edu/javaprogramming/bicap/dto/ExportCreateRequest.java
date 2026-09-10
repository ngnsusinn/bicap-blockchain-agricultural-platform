package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class ExportCreateRequest {

    @NotNull(message = "Export date is required")
    private LocalDate exportDate;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be positive")
    private Double quantity;

    @NotBlank(message = "Destination is required")
    @Size(max = 500, message = "Destination must not exceed 500 characters")
    private String destination;

    public ExportCreateRequest() {
    }

    public ExportCreateRequest(LocalDate exportDate, Double quantity, String destination) {
        this.exportDate = exportDate;
        this.quantity = quantity;
        this.destination = destination;
    }

    public LocalDate getExportDate() {
        return exportDate;
    }

    public void setExportDate(LocalDate exportDate) {
        this.exportDate = exportDate;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}
