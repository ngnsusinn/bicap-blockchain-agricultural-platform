package vn.courses.ut.edu.javaprogramming.bicap.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SeasonResponse {
    private Long id;
    private Long farmId;
    private String name;
    private String productType;
    private String variety;
    private Double area;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String txHash;
    private LocalDateTime createdAt;

    public SeasonResponse() {
    }

    public SeasonResponse(Long id, Long farmId, String name, String productType, String variety, Double area, LocalDate startDate, LocalDate endDate, String status, String txHash, LocalDateTime createdAt) {
        this.id = id;
        this.farmId = farmId;
        this.name = name;
        this.productType = productType;
        this.variety = variety;
        this.area = area;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.txHash = txHash;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }

    public Double getArea() { return area; }
    public void setArea(Double area) { this.area = area; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTxHash() { return txHash; }
    public void setTxHash(String txHash) { this.txHash = txHash; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long farmId;
        private String name;
        private String productType;
        private String variety;
        private Double area;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
        private String txHash;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder farmId(Long farmId) { this.farmId = farmId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder productType(String productType) { this.productType = productType; return this; }
        public Builder variety(String variety) { this.variety = variety; return this; }
        public Builder area(Double area) { this.area = area; return this; }
        public Builder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public Builder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder txHash(String txHash) { this.txHash = txHash; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public SeasonResponse build() {
            return new SeasonResponse(id, farmId, name, productType, variety, area, startDate, endDate, status, txHash, createdAt);
        }
    }
}
