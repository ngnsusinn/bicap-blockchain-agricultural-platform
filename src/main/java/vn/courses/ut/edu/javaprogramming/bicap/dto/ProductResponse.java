package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.entity.Category;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Summary payload for a product in the admin monitoring list (BICAP-5 / SRS-ADM-004).
 * Carries denormalized display names (category, season, farm) resolved by the service.
 */
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Double quantity;
    private Long categoryId;
    private String categoryName;
    private Long seasonId;
    private String seasonName;
    private Long farmId;
    private String farmName;
    private Long qrCodeId;
    private String status;
    private LocalDateTime createdAt;

    public ProductResponse() {}

    public ProductResponse(Long id, String name, String description, BigDecimal price, Double quantity,
                           Long categoryId, String categoryName, Long seasonId, String seasonName,
                           Long farmId, String farmName, Long qrCodeId, String status, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.seasonId = seasonId;
        this.seasonName = seasonName;
        this.farmId = farmId;
        this.farmName = farmName;
        this.qrCodeId = qrCodeId;
        this.status = status;
        this.createdAt = createdAt;
    }

    /** Creates a response from a product entity; display names resolved from lookups (nullable → "—"). */
    public static ProductResponse fromEntity(Product product, Category category, FarmingSeason season, Farm farm) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantity(),
                product.getCategoryId(),
                category != null ? category.getName() : null,
                product.getSeasonId(),
                season != null ? season.getName() : null,
                farm != null ? farm.getId() : null,
                farm != null ? farm.getName() : null,
                product.getQrCodeId(),
                product.getStatus(),
                product.getCreatedAt()
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Long getSeasonId() { return seasonId; }
    public void setSeasonId(Long seasonId) { this.seasonId = seasonId; }
    public String getSeasonName() { return seasonName; }
    public void setSeasonName(String seasonName) { this.seasonName = seasonName; }
    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }
    public String getFarmName() { return farmName; }
    public void setFarmName(String farmName) { this.farmName = farmName; }
    public Long getQrCodeId() { return qrCodeId; }
    public void setQrCodeId(Long qrCodeId) { this.qrCodeId = qrCodeId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
