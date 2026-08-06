package vn.courses.ut.edu.javaprogramming.bicap.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A single process step within a farming season (BICAP-15 / SRS-FM-009).
 * Maps to the `farming_processes` table.
 * tx_hash is populated after the process is written to VeChainThor Blockchain (BICAP-15).
 *
 * materials and images are stored as JSON strings, e.g.:
 *   materials: [{"name":"Ure","amount":50,"unit":"kg"},...]
 *   images:    ["https://cdn.../img1.jpg","https://cdn.../img2.jpg"]
 */
@Entity
@Table(name = "farming_processes")
public class FarmingProcess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "season_id", nullable = false)
    private Long seasonId;

    @Column(name = "process_type", nullable = false, length = 100)
    private String processType;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "performed_by", length = 255)
    private String performedBy;

    @Column(name = "execution_date", nullable = false)
    private LocalDate executionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProcessStatus status = ProcessStatus.COMPLETED;

    /** JSON string — vật tư sử dụng, e.g. [{name, amount, unit}] */
    @Column(columnDefinition = "JSON")
    private String materials;

    /** JSON string — danh sách URL ảnh minh chứng */
    @Column(columnDefinition = "JSON")
    private String images;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** VeChainThor transaction hash — null until blockchain write succeeds (BICAP-15). */
    @Column(name = "tx_hash", length = 66)
    private String txHash;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public FarmingProcess() {}

    public FarmingProcess(Long id, Long seasonId, String processType, LocalDate executionDate,
                          String materials, String images, String notes,
                          String txHash, LocalDateTime createdAt) {
        this.id = id;
        this.seasonId = seasonId;
        this.processType = processType;
        this.executionDate = executionDate;
        this.materials = materials;
        this.images = images;
        this.notes = notes;
        this.txHash = txHash;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = ProcessStatus.COMPLETED;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSeasonId() { return seasonId; }
    public void setSeasonId(Long seasonId) { this.seasonId = seasonId; }
    public String getProcessType() { return processType; }
    public void setProcessType(String processType) { this.processType = processType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }
    public LocalDate getExecutionDate() { return executionDate; }
    public void setExecutionDate(LocalDate executionDate) { this.executionDate = executionDate; }
    public ProcessStatus getStatus() { return status; }
    public void setStatus(ProcessStatus status) { this.status = status; }
    public String getMaterials() { return materials; }
    public void setMaterials(String materials) { this.materials = materials; }
    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getTxHash() { return txHash; }
    public void setTxHash(String txHash) { this.txHash = txHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static FarmingProcessBuilder builder() { return new FarmingProcessBuilder(); }

    public static class FarmingProcessBuilder {
        private Long id;
        private Long seasonId;
        private String processType;
        private LocalDate executionDate;
        private String materials;
        private String images;
        private String notes;
        private String txHash;
        private LocalDateTime createdAt;

        FarmingProcessBuilder() {}

        public FarmingProcessBuilder id(Long id) { this.id = id; return this; }
        public FarmingProcessBuilder seasonId(Long seasonId) { this.seasonId = seasonId; return this; }
        public FarmingProcessBuilder processType(String processType) { this.processType = processType; return this; }
        public FarmingProcessBuilder executionDate(LocalDate executionDate) { this.executionDate = executionDate; return this; }
        public FarmingProcessBuilder materials(String materials) { this.materials = materials; return this; }
        public FarmingProcessBuilder images(String images) { this.images = images; return this; }
        public FarmingProcessBuilder notes(String notes) { this.notes = notes; return this; }
        public FarmingProcessBuilder txHash(String txHash) { this.txHash = txHash; return this; }
        public FarmingProcessBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public FarmingProcess build() {
            return new FarmingProcess(id, seasonId, processType, executionDate,
                    materials, images, notes, txHash, createdAt);
        }
    }
}
