package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * Request payload for adding a process step to a farming season (BICAP-15 / SRS-FM-009).
 *
 * processType: SOIL_PREP | SEEDING | FERTILIZATION | PEST_CONTROL | HARVESTING | OTHER
 * materials:   JSON string — vật tư sử dụng, e.g. [{"name":"Ure","amount":50,"unit":"kg"}]
 * images:      JSON string — danh sách URL ảnh minh chứng, e.g. ["https://cdn/.../img.jpg"]
 */
public class ProcessRequest {

    @NotBlank(message = "Loại quy trình không được để trống")
    @Size(max = 100, message = "Loại quy trình không vượt quá 100 ký tự")
    private String processType;

    @NotBlank(message = "Tên quy trình không được để trống")
    @Size(max = 255, message = "Tên quy trình không vượt quá 255 ký tự")
    private String name;

    @Size(max = 2000, message = "Mô tả không vượt quá 2000 ký tự")
    private String description;

    @NotNull(message = "Ngày thực hiện không được để trống")
    private LocalDate executionDate;

    @NotBlank(message = "Trạng thái quy trình không được để trống")
    @Pattern(regexp = "PENDING|COMPLETED|CANCELLED", message = "Trạng thái quy trình không hợp lệ")
    private String status = "COMPLETED";

    /** JSON string — optional */
    private String materials;

    /** JSON string — optional */
    private String images;

    @Size(max = 2000, message = "Ghi chú không vượt quá 2000 ký tự")
    private String notes;

    public ProcessRequest() {}

    public ProcessRequest(String processType, LocalDate executionDate,
                          String materials, String images, String notes) {
        this.processType = processType;
        this.executionDate = executionDate;
        this.materials = materials;
        this.images = images;
        this.notes = notes;
    }

    public String getProcessType() { return processType; }
    public void setProcessType(String processType) { this.processType = processType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getExecutionDate() { return executionDate; }
    public void setExecutionDate(LocalDate executionDate) { this.executionDate = executionDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMaterials() { return materials; }
    public void setMaterials(String materials) { this.materials = materials; }
    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
