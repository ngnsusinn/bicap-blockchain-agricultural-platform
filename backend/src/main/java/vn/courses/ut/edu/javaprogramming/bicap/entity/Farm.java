package vn.courses.ut.edu.javaprogramming.bicap.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Farm registration profile (BICAP-3 / BICAP-9) and managed farm record (BICAP-4 / SRS-ADM-003).
 * Maps to the `farms` table — a registration record owned by a Farm Manager user.
 */
@Entity
@Table(name = "farms")
public class Farm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private Double area;

    @Column(name = "gps_lat")
    private Double gpsLat;

    @Column(name = "gps_lng")
    private Double gpsLng;

    @Column(length = 2000)
    private String description;

    /** Comma-separated product types, e.g. "rau, củ, quả" (BICAP-4 display). */
    @Column(name = "product_types", length = 500)
    private String productTypes;

    /** Internal admin notes (SRS-ADM-003, max 2000 chars). */
    @Column(name = "admin_notes", length = 2000)
    private String adminNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FarmStatus status = FarmStatus.PENDING;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Farm() {
    }

    public Farm(Long id, Long userId, String name, String address, Double area, Double gpsLat, Double gpsLng,
                String description, String productTypes, String adminNotes, FarmStatus status,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.address = address;
        this.area = area;
        this.gpsLat = gpsLat;
        this.gpsLng = gpsLng;
        this.description = description;
        this.productTypes = productTypes;
        this.adminNotes = adminNotes;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
        if (this.status == null) {
            this.status = FarmStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getArea() { return area; }
    public void setArea(Double area) { this.area = area; }
    public Double getGpsLat() { return gpsLat; }
    public void setGpsLat(Double gpsLat) { this.gpsLat = gpsLat; }
    public Double getGpsLng() { return gpsLng; }
    public void setGpsLng(Double gpsLng) { this.gpsLng = gpsLng; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getProductTypes() { return productTypes; }
    public void setProductTypes(String productTypes) { this.productTypes = productTypes; }
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
    public FarmStatus getStatus() { return status; }
    public void setStatus(FarmStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static FarmBuilder builder() {
        return new FarmBuilder();
    }

    public static class FarmBuilder {
        private Long id;
        private Long userId;
        private String name;
        private String address;
        private Double area;
        private Double gpsLat;
        private Double gpsLng;
        private String description;
        private String productTypes;
        private String adminNotes;
        private FarmStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        FarmBuilder() {}

        public FarmBuilder id(Long id) { this.id = id; return this; }
        public FarmBuilder userId(Long userId) { this.userId = userId; return this; }
        public FarmBuilder name(String name) { this.name = name; return this; }
        public FarmBuilder address(String address) { this.address = address; return this; }
        public FarmBuilder area(Double area) { this.area = area; return this; }
        public FarmBuilder gpsLat(Double gpsLat) { this.gpsLat = gpsLat; return this; }
        public FarmBuilder gpsLng(Double gpsLng) { this.gpsLng = gpsLng; return this; }
        public FarmBuilder description(String description) { this.description = description; return this; }
        public FarmBuilder productTypes(String productTypes) { this.productTypes = productTypes; return this; }
        public FarmBuilder adminNotes(String adminNotes) { this.adminNotes = adminNotes; return this; }
        public FarmBuilder status(FarmStatus status) { this.status = status; return this; }
        public FarmBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public FarmBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Farm build() {
            return new Farm(id, userId, name, address, area, gpsLat, gpsLng,
                    description, productTypes, adminNotes, status, createdAt, updatedAt);
        }
    }
}
