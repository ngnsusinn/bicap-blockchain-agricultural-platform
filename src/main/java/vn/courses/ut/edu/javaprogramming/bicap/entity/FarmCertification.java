package vn.courses.ut.edu.javaprogramming.bicap.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Business license / certification document attached to a farm registration (BICAP-3).
 * Maps to the `farm_certifications` table.
 */
@Entity
@Table(name = "farm_certifications")
public class FarmCertification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "farm_id", nullable = false)
    private Long farmId;

    @Column(nullable = false)
    private String type;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public FarmCertification() {
    }

    public FarmCertification(Long id, Long farmId, String type, String fileUrl, LocalDate expiryDate, LocalDateTime createdAt) {
        this.id = id;
        this.farmId = farmId;
        this.type = type;
        this.fileUrl = fileUrl;
        this.expiryDate = expiryDate;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static FarmCertificationBuilder builder() {
        return new FarmCertificationBuilder();
    }

    public static class FarmCertificationBuilder {
        private Long id;
        private Long farmId;
        private String type;
        private String fileUrl;
        private LocalDate expiryDate;
        private LocalDateTime createdAt;

        FarmCertificationBuilder() {}

        public FarmCertificationBuilder id(Long id) { this.id = id; return this; }
        public FarmCertificationBuilder farmId(Long farmId) { this.farmId = farmId; return this; }
        public FarmCertificationBuilder type(String type) { this.type = type; return this; }
        public FarmCertificationBuilder fileUrl(String fileUrl) { this.fileUrl = fileUrl; return this; }
        public FarmCertificationBuilder expiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; return this; }
        public FarmCertificationBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public FarmCertification build() {
            return new FarmCertification(id, farmId, type, fileUrl, expiryDate, createdAt);
        }
    }
}
