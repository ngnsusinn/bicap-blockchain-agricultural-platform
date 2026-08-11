package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class ProcessUpdateRequest {

    @Size(max = 100, message = "Process type must not exceed 100 characters")
    private String processType;

    private LocalDate executionDate;

    private String materials;

    private String images;

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;

    public ProcessUpdateRequest() {
    }

    public ProcessUpdateRequest(String processType, LocalDate executionDate, String materials, String images, String notes) {
        this.processType = processType;
        this.executionDate = executionDate;
        this.materials = materials;
        this.images = images;
        this.notes = notes;
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
}
