package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Approval decision for a farm registration (BICAP-3 / SRS-ADM-002).
 * action: APPROVE or REJECT — reason is mandatory when REJECT.
 */
public class FarmApprovalRequest {

    @NotBlank(message = "Action is required")
    @Pattern(regexp = "APPROVE|REJECT", message = "Action must be APPROVE or REJECT")
    private String action;

    @Size(max = 1000, message = "Reason must not exceed 1000 characters")
    private String reason;

    public FarmApprovalRequest() {}

    public FarmApprovalRequest(String action, String reason) {
        this.action = action;
        this.reason = reason;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public boolean isApprove() { return "APPROVE".equalsIgnoreCase(action); }
    public boolean isReject() { return "REJECT".equalsIgnoreCase(action); }
}
