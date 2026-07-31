package vn.courses.ut.edu.javaprogramming.bicap.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_packages")
public class ServicePackage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String description;
    private BigDecimal price;
    
    @Column(name = "duration_days")
    private int durationDays;
    
    @Column(columnDefinition = "json")
    private String features;
    
    private String status;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public ServicePackage() {
    }

    public ServicePackage(Long id, String name, String description, BigDecimal price, int durationDays, String features, String status, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.durationDays = durationDays;
        this.features = features;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }
    public String getFeatures() { return features; }
    public void setFeatures(String features) { this.features = features; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private int durationDays;
        private String features;
        private String status;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder price(BigDecimal price) { this.price = price; return this; }
        public Builder durationDays(int durationDays) { this.durationDays = durationDays; return this; }
        public Builder features(String features) { this.features = features; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        
        public ServicePackage build() {
            return new ServicePackage(id, name, description, price, durationDays, features, status, createdAt);
        }
    }
}
