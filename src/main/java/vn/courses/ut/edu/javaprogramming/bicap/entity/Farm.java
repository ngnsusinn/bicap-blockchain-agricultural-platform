package vn.courses.ut.edu.javaprogramming.bicap.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Farm registration profile (BICAP-3 / BICAP-9).
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

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private Double area;

    @Column(name = "gps_lat")
    private Double gpsLat;

    @Column(name = "gps_lng")
    private Double gpsLng;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FarmStatus status = FarmStatus.PENDING;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Farm() {
    }

    public Farm(Long id, Long userId, String name, String address, Double area, Double gpsLat, Double gpsLng, FarmStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.address = address;
        this.area = area;
        this.gpsLat = gpsLat;
        this.gpsLng = gpsLng;
        this.status = status;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = FarmStatus.PENDING;
        }
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
    public FarmStatus getStatus() { return status; }
    public void setStatus(FarmStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

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
        private FarmStatus status;
        private LocalDateTime createdAt;

        FarmBuilder() {}

        public FarmBuilder id(Long id) { this.id = id; return this; }
        public FarmBuilder userId(Long userId) { this.userId = userId; return this; }
        public FarmBuilder name(String name) { this.name = name; return this; }
        public FarmBuilder address(String address) { this.address = address; return this; }
        public FarmBuilder area(Double area) { this.area = area; return this; }
        public FarmBuilder gpsLat(Double gpsLat) { this.gpsLat = gpsLat; return this; }
        public FarmBuilder gpsLng(Double gpsLng) { this.gpsLng = gpsLng; return this; }
        public FarmBuilder status(FarmStatus status) { this.status = status; return this; }
        public FarmBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Farm build() {
            return new Farm(id, userId, name, address, area, gpsLat, gpsLng, status, createdAt);
        }
    }
}
