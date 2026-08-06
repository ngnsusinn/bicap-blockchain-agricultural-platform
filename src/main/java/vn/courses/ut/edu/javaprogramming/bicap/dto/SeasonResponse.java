package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.SeasonStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response payload for a farming season (BICAP-12/13/14 / SRS-FM-006/007/008).
 * processCount is populated for list views; processes list is populated for detail view.
 */
public class SeasonResponse {

    private Long id;
    private Long farmId;
    private String name;
    private String description;
    private String productType;
    private String variety;
    private Double area;
    private LocalDate startDate;
    private LocalDate endDate;
    private String notes;
    private SeasonStatus status;
    /** VeChainThor transaction hash (null when blockchain write is pending). */
    private String txHash;
    private LocalDateTime createdAt;
    /** Number of process steps — populated for list view. */
    private Long processCount;
    /** Full process list — populated for detail view only. */
    private List<ProcessResponse> processes;

    public SeasonResponse() {}

    public static SeasonResponse fromEntity(FarmingSeason s) {
        SeasonResponse r = new SeasonResponse();
        r.id = s.getId();
        r.farmId = s.getFarmId();
        r.name = s.getName();
        r.description = s.getDescription();
        r.productType = s.getProductType();
        r.variety = s.getVariety();
        r.area = s.getArea();
        r.startDate = s.getStartDate();
        r.endDate = s.getEndDate();
        r.notes = s.getNotes();
        r.status = s.getStatus();
        r.txHash = s.getTxHash();
        r.createdAt = s.getCreatedAt();
        return r;
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
    public Long getProcessCount() { return processCount; }
    public void setProcessCount(Long processCount) { this.processCount = processCount; }
    public List<ProcessResponse> getProcesses() { return processes; }
    public void setProcesses(List<ProcessResponse> processes) { this.processes = processes; }
}
