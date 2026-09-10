package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Product status change payload (BICAP-5 / SRS-ADM-004).
 * Statuses: ACTIVE, INACTIVE, PENDING_REVIEW.
 */
public class ProductStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "ACTIVE|INACTIVE|PENDING_REVIEW",
             message = "Status must be ACTIVE, INACTIVE or PENDING_REVIEW")
    private String status;

    public ProductStatusUpdateRequest() {}

    public ProductStatusUpdateRequest(String status) {
        this.status = status;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
