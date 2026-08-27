package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.NotNull;

public class PurchasePackageRequest {
    @NotNull
    private Long packageId;

    public PurchasePackageRequest() {
    }

    public PurchasePackageRequest(Long packageId) {
        this.packageId = packageId;
    }

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }
}
