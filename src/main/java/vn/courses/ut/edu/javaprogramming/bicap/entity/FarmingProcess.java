package vn.courses.ut.edu.javaprogramming.bicap.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "farming_processes")
public class FarmingProcess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "season_id", nullable = false)
    private Long seasonId;

    @Column(name = "process_type", nullable = false, length = 100)
    private String processType; // SOIL_PREP, SEEDING, FERTILIZATION, PEST_CONTROL, HARVESTING

    @Column(name = "execution_date", nullable = false)
    private LocalDate executionDate;

    @Column(columnDefinition = "TEXT")
    private String materials; // Stores JSON as string

    @Column(columnDefinition = "TEXT")
    private String images; // Stores JSON array of URLs as string

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "tx_hash", length = 66)
    private String txHash;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public FarmingProcess() {
    }

    public FarmingProcess(Long id, Long seasonId, String processType, LocalDate executionDate,
                          String materials, String images, String notes, String txHash,
                          LocalDateTime createdAt) {
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
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSeasonId() {
        return seasonId;
    }

    public void setSeasonId(Long seasonId) {
        this.seasonId = seasonId;
    }

    public String getProcessType() {
        return processType;
    }

    public void setProcessType(String processType) {
        this.processType = processType;
    }

    public LocalDate getExecutionDate() {
        return executionDate;
    }

    public void setExecutionDate(LocalDate executionDate) {
        this.executionDate = executionDate;
    }

    public String getMaterials() {
        return materials;
    }

    public void setMaterials(String materials) {
        this.materials = materials;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
