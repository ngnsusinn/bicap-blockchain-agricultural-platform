package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmCertification;

import java.time.LocalDate;

/**
 * Response payload for a certification/business-license document of a farm (BICAP-3).
 */
public class FarmCertificationResponse {
    private Long id;
    private String type;
    private String fileUrl;
    private LocalDate expiryDate;

    public FarmCertificationResponse() {}

    public FarmCertificationResponse(Long id, String type, String fileUrl, LocalDate expiryDate) {
        this.id = id;
        this.type = type;
        this.fileUrl = fileUrl;
        this.expiryDate = expiryDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public static FarmCertificationResponse fromEntity(FarmCertification cert) {
        if (cert == null) return null;
        return new FarmCertificationResponse(cert.getId(), cert.getType(), cert.getFileUrl(), cert.getExpiryDate());
    }

    public static FarmCertificationResponseBuilder builder() {
        return new FarmCertificationResponseBuilder();
    }

    public static class FarmCertificationResponseBuilder {
        private Long id;
        private String type;
        private String fileUrl;
        private LocalDate expiryDate;

        FarmCertificationResponseBuilder() {}

        public FarmCertificationResponseBuilder id(Long id) { this.id = id; return this; }
        public FarmCertificationResponseBuilder type(String type) { this.type = type; return this; }
        public FarmCertificationResponseBuilder fileUrl(String fileUrl) { this.fileUrl = fileUrl; return this; }
        public FarmCertificationResponseBuilder expiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; return this; }

        public FarmCertificationResponse build() {
            return new FarmCertificationResponse(id, type, fileUrl, expiryDate);
        }
    }
}
