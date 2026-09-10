package vn.courses.ut.edu.javaprogramming.bicap.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "farming_seasons")
public class FarmingSeason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "farm_id", nullable = false)
    private Long farmId;

    @Column(nullable = false)
    private String name;

    @Column(name = "product_type", nullable = false, length = 100)
    private String productType;

    @Column(nullable = false, length = 100)
    private String variety;

    @Column(nullable = false)
    private Double area;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false, length = 20)
    private String status = "IN_PROGRESS"; // IN_PROGRESS, HARVESTED, CANCELLED

    /** Total harvested amount recorded when the season transitions to HARVESTED (BICAP-16). */
    @Column(name = "harvested_quantity", precision = 16, scale = 2)
    private java.math.BigDecimal harvestedQuantity;

    @Column(name = "harvest_unit", length = 30)
    private String harvestUnit;

    @Column(name = "tx_hash", length = 66)
    private String txHash;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public FarmingSeason() {
    }

    public FarmingSeason(Long id, Long farmId, String name, String productType, String variety,
                         Double area, LocalDate startDate, LocalDate endDate, String status,
                         String txHash, LocalDateTime createdAt) {
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

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "IN_PROGRESS";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFarmId() {
        return farmId;
    }

    public void setFarmId(Long farmId) {
        this.farmId = farmId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.math.BigDecimal getHarvestedQuantity() {
        return harvestedQuantity;
    }

    public void setHarvestedQuantity(java.math.BigDecimal harvestedQuantity) {
        this.harvestedQuantity = harvestedQuantity;
    }

    public String getHarvestUnit() {
        return harvestUnit;
    }

    public void setHarvestUnit(String harvestUnit) {
        this.harvestUnit = harvestUnit;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
