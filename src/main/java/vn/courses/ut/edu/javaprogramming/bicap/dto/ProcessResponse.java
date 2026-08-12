package vn.courses.ut.edu.javaprogramming.bicap.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ProcessResponse {
    private Long id;
    private Long seasonId;
    private String processType;
    private LocalDate executionDate;
    private String materials;
    private String images;
    private String notes;
    private String txHash;
    private LocalDateTime createdAt;

    public ProcessResponse() {
    }

    public ProcessResponse(Long id, Long seasonId, String processType, LocalDate executionDate, String materials, String images, String notes, String txHash, LocalDateTime createdAt) {
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSeasonId() { return seasonId; }
    public void setSeasonId(Long seasonId) { this.seasonId = seasonId; }

    public String getProcessType() { return processType; }
    public void setProcessType(String processType) { this.processType = processType; }

    public LocalDate getExecutionDate() { return executionDate; }
    public void setExecutionDate(LocalDate executionDate) { this.executionDate = executionDate; }

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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long seasonId;
        private String processType;
        private LocalDate executionDate;
        private String materials;
        private String images;
        private String notes;
        private String txHash;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder seasonId(Long seasonId) { this.seasonId = seasonId; return this; }
        public Builder processType(String processType) { this.processType = processType; return this; }
        public Builder executionDate(LocalDate executionDate) { this.executionDate = executionDate; return this; }
        public Builder materials(String materials) { this.materials = materials; return this; }
        public Builder images(String images) { this.images = images; return this; }
        public Builder notes(String notes) { this.notes = notes; return this; }
        public Builder txHash(String txHash) { this.txHash = txHash; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ProcessResponse build() {
            return new ProcessResponse(id, seasonId, processType, executionDate, materials, images, notes, txHash, createdAt);
        }
    }
}
