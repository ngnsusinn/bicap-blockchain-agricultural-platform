package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.NotNull;

public class PurchasePackageRequest {
    @NotNull
    private Long packageId;

    @NotNull
    private Long farmId;

    public PurchasePackageRequest() {
    }

    public PurchasePackageRequest(Long packageId, Long farmId) {
        this.packageId = packageId;
        this.farmId = farmId;
    }

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }
    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }
}
