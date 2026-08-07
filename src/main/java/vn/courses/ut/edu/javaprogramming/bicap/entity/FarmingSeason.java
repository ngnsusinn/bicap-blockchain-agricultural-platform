package vn.courses.ut.edu.javaprogramming.bicap.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

<<<<<<< HEAD
/**
 * A farming season owned by a farm (BICAP-12/13/14 / SRS-FM-006/007/008).
 * Maps to the `farming_seasons` table.
 * tx_hash is populated after the season is written to VeChainThor Blockchain (BICAP-14).
 */
=======
>>>>>>> origin/main
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

<<<<<<< HEAD
    @Column(columnDefinition = "TEXT")
    private String description;

=======
>>>>>>> origin/main
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

<<<<<<< HEAD
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeasonStatus status = SeasonStatus.IN_PROGRESS;

    /** VeChainThor transaction hash — null until blockchain write succeeds (BICAP-14). */
=======
    @Column(nullable = false, length = 20)
    private String status = "IN_PROGRESS"; // IN_PROGRESS, HARVESTED, CANCELLED

>>>>>>> origin/main
    @Column(name = "tx_hash", length = 66)
    private String txHash;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

<<<<<<< HEAD
    public FarmingSeason() {}

    public FarmingSeason(Long id, Long farmId, String name, String productType, String variety,
                         Double area, LocalDate startDate, LocalDate endDate,
                         SeasonStatus status, String txHash, LocalDateTime createdAt) {
=======
    public FarmingSeason() {
    }

    public FarmingSeason(Long id, Long farmId, String name, String productType, String variety,
                         Double area, LocalDate startDate, LocalDate endDate, String status,
                         String txHash, LocalDateTime createdAt) {
>>>>>>> origin/main
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
<<<<<<< HEAD
            this.status = SeasonStatus.IN_PROGRESS;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
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
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public SeasonStatus getStatus() { return status; }
    public void setStatus(SeasonStatus status) { this.status = status; }
    public String getTxHash() { return txHash; }
    public void setTxHash(String txHash) { this.txHash = txHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static FarmingSeasonBuilder builder() { return new FarmingSeasonBuilder(); }

    public static class FarmingSeasonBuilder {
        private Long id;
        private Long farmId;
        private String name;
        private String productType;
        private String variety;
        private Double area;
        private LocalDate startDate;
        private LocalDate endDate;
        private SeasonStatus status;
        private String txHash;
        private LocalDateTime createdAt;

        FarmingSeasonBuilder() {}

        public FarmingSeasonBuilder id(Long id) { this.id = id; return this; }
        public FarmingSeasonBuilder farmId(Long farmId) { this.farmId = farmId; return this; }
        public FarmingSeasonBuilder name(String name) { this.name = name; return this; }
        public FarmingSeasonBuilder productType(String productType) { this.productType = productType; return this; }
        public FarmingSeasonBuilder variety(String variety) { this.variety = variety; return this; }
        public FarmingSeasonBuilder area(Double area) { this.area = area; return this; }
        public FarmingSeasonBuilder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public FarmingSeasonBuilder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public FarmingSeasonBuilder status(SeasonStatus status) { this.status = status; return this; }
        public FarmingSeasonBuilder txHash(String txHash) { this.txHash = txHash; return this; }
        public FarmingSeasonBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public FarmingSeason build() {
            return new FarmingSeason(id, farmId, name, productType, variety, area,
                    startDate, endDate, status, txHash, createdAt);
        }
=======
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
>>>>>>> origin/main
    }
}
