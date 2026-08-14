package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SeasonStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "HARVESTED|CANCELLED", message = "Status must be HARVESTED or CANCELLED")
    private String status;

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
}
