package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmStatus;

import java.time.LocalDateTime;

/**
 * Summary payload for a farm registration in the admin approval list (BICAP-3 / SRS-ADM-002).
 */
public class FarmResponse {
    private Long id;
    private String name;
    private String address;
    private Double area;
    private Double gpsLat;
    private Double gpsLng;
    private FarmStatus status;
    private LocalDateTime createdAt;
    private String ownerName;
    private String ownerEmail;
    private String ownerPhone;
    private Integer certificationCount;

    public FarmResponse() {}

    public FarmResponse(Long id, String name, String address, Double area, Double gpsLat, Double gpsLng,
                        FarmStatus status, LocalDateTime createdAt, String ownerName, String ownerEmail,
                        String ownerPhone, Integer certificationCount) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.area = area;
        this.gpsLat = gpsLat;
        this.gpsLng = gpsLng;
        this.status = status;
        this.createdAt = createdAt;
        this.ownerName = ownerName;
        this.ownerEmail = ownerEmail;
        this.ownerPhone = ownerPhone;
        this.certificationCount = certificationCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
    public String getOwnerPhone() { return ownerPhone; }
    public void setOwnerPhone(String ownerPhone) { this.ownerPhone = ownerPhone; }
    public Integer getCertificationCount() { return certificationCount; }
    public void setCertificationCount(Integer certificationCount) { this.certificationCount = certificationCount; }

    public static FarmResponseBuilder builder() {
        return new FarmResponseBuilder();
    }

    public static class FarmResponseBuilder {
        private Long id;
        private String name;
        private String address;
        private Double area;
        private Double gpsLat;
        private Double gpsLng;
        private FarmStatus status;
        private LocalDateTime createdAt;
        private String ownerName;
        private String ownerEmail;
        private String ownerPhone;
        private Integer certificationCount;

        FarmResponseBuilder() {}

        public FarmResponseBuilder id(Long id) { this.id = id; return this; }
        public FarmResponseBuilder name(String name) { this.name = name; return this; }
        public FarmResponseBuilder address(String address) { this.address = address; return this; }
        public FarmResponseBuilder area(Double area) { this.area = area; return this; }
        public FarmResponseBuilder gpsLat(Double gpsLat) { this.gpsLat = gpsLat; return this; }
        public FarmResponseBuilder gpsLng(Double gpsLng) { this.gpsLng = gpsLng; return this; }
        public FarmResponseBuilder status(FarmStatus status) { this.status = status; return this; }
        public FarmResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public FarmResponseBuilder ownerName(String ownerName) { this.ownerName = ownerName; return this; }
        public FarmResponseBuilder ownerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; return this; }
        public FarmResponseBuilder ownerPhone(String ownerPhone) { this.ownerPhone = ownerPhone; return this; }
        public FarmResponseBuilder certificationCount(Integer certificationCount) { this.certificationCount = certificationCount; return this; }

        public FarmResponse build() {
            return new FarmResponse(id, name, address, area, gpsLat, gpsLng, status, createdAt,
                    ownerName, ownerEmail, ownerPhone, certificationCount);
        }
    }
}
