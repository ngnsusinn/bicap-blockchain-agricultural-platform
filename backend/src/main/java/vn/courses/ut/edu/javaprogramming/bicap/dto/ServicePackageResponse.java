package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.entity.ServicePackage;
import java.math.BigDecimal;

public class ServicePackageResponse {
    private final Long id;
    private final String name;
    private final String description;
    private final BigDecimal price;
    private final int durationDays;
    private final String features;
    private final String status;

    public ServicePackageResponse(Long id, String name, String description, BigDecimal price, int durationDays, String features, String status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.durationDays = durationDays;
        this.features = features;
        this.status = status;
    }

    public static ServicePackageResponse fromEntity(ServicePackage pkg) {
        return new ServicePackageResponse(
            pkg.getId(),
            pkg.getName(),
            pkg.getDescription(),
            pkg.getPrice(),
            pkg.getDurationDays(),
            pkg.getFeatures(),
            pkg.getStatus()
        );
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public int getDurationDays() { return durationDays; }
    public String getFeatures() { return features; }
    public String getStatus() { return status; }
}
