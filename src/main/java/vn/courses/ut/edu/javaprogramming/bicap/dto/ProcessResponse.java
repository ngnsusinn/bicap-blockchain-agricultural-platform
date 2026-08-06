package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingProcess;
import vn.courses.ut.edu.javaprogramming.bicap.entity.ProcessStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response payload for a single process step (BICAP-12/15 / SRS-FM-006/009).
 */
public class ProcessResponse {

    private Long id;
    private Long seasonId;
    private String processType;
    private String name;
    private String description;
    private String performedBy;
    private LocalDate executionDate;
    private ProcessStatus status;
    /** JSON string of materials used. */
    private String materials;
    /** JSON string of image URLs. */
    private String images;
    private String notes;
    /** VeChainThor transaction hash (null when blockchain write is pending). */
    private String txHash;
    private LocalDateTime createdAt;

    public ProcessResponse() {}

    public static ProcessResponse fromEntity(FarmingProcess p) {
        ProcessResponse r = new ProcessResponse();
        r.id = p.getId();
        r.seasonId = p.getSeasonId();
        r.processType = p.getProcessType();
        r.name = p.getName();
        r.description = p.getDescription();
        r.performedBy = p.getPerformedBy();
        r.executionDate = p.getExecutionDate();
        r.status = p.getStatus();
        r.materials = p.getMaterials();
        r.images = p.getImages();
        r.notes = p.getNotes();
        r.txHash = p.getTxHash();
        r.createdAt = p.getCreatedAt();
        return r;
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
}
