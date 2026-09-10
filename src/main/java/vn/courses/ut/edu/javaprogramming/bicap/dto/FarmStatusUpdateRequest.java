package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Operating status change for a managed farm (BICAP-4 / SRS-ADM-003).
 * Management statuses: APPROVED (active), SUSPENDED, INACTIVE.
 */
public class FarmStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "APPROVED|SUSPENDED|INACTIVE", message = "Status must be APPROVED, SUSPENDED or INACTIVE")
    private String status;

    public FarmStatusUpdateRequest() {}

    public FarmStatusUpdateRequest(String status) {
        this.status = status;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
