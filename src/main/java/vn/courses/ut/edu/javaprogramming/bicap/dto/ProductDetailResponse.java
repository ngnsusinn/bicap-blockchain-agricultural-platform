package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.entity.Category;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Product;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Full product detail for the admin monitoring view (BICAP-5 / SRS-ADM-004).
 * Extends the list summary with season, farm and owner context.
 */
public class ProductDetailResponse extends ProductResponse {
    private LocalDate seasonStartDate;
    private LocalDate seasonEndDate;
    private String seasonProductType;
    private String seasonVariety;
    private String farmAddress;
    private String ownerName;
    private String ownerEmail;

    public ProductDetailResponse() {}

    public ProductDetailResponse(Long id, String name, String description, BigDecimal price, Double quantity,
                                 Long categoryId, String categoryName, Long seasonId, String seasonName,
                                 Long farmId, String farmName, Long qrCodeId, String status, LocalDateTime createdAt,
                                 LocalDate seasonStartDate, LocalDate seasonEndDate, String seasonProductType,
                                 String seasonVariety, String farmAddress, String ownerName, String ownerEmail) {
        super(id, name, description, price, quantity, categoryId, categoryName, seasonId, seasonName,
                farmId, farmName, qrCodeId, status, createdAt);
        this.seasonStartDate = seasonStartDate;
        this.seasonEndDate = seasonEndDate;
        this.seasonProductType = seasonProductType;
        this.seasonVariety = seasonVariety;
        this.farmAddress = farmAddress;
        this.ownerName = ownerName;
        this.ownerEmail = ownerEmail;
    }

    public static ProductDetailResponse fromEntity(Product product, Category category, FarmingSeason season,
                                                   Farm farm, User owner) {
        return new ProductDetailResponse(
                product.getId(), product.getName(), product.getDescription(), product.getPrice(),
                product.getQuantity(), product.getCategoryId(),
                category != null ? category.getName() : null,
                product.getSeasonId(), season != null ? season.getName() : null,
                farm != null ? farm.getId() : null, farm != null ? farm.getName() : null,
                product.getQrCodeId(), product.getStatus(), product.getCreatedAt(),
                season != null ? season.getStartDate() : null,
                season != null ? season.getEndDate() : null,
                season != null ? season.getProductType() : null,
                season != null ? season.getVariety() : null,
                farm != null ? farm.getAddress() : null,
                owner != null ? owner.getFullName() : null,
                owner != null ? owner.getEmail() : null
        );
    }

    public LocalDate getSeasonStartDate() { return seasonStartDate; }
    public void setSeasonStartDate(LocalDate seasonStartDate) { this.seasonStartDate = seasonStartDate; }
    public LocalDate getSeasonEndDate() { return seasonEndDate; }
    public void setSeasonEndDate(LocalDate seasonEndDate) { this.seasonEndDate = seasonEndDate; }
    public String getSeasonProductType() { return seasonProductType; }
    public void setSeasonProductType(String seasonProductType) { this.seasonProductType = seasonProductType; }
    public String getSeasonVariety() { return seasonVariety; }
    public void setSeasonVariety(String seasonVariety) { this.seasonVariety = seasonVariety; }
    public String getFarmAddress() { return farmAddress; }
    public void setFarmAddress(String farmAddress) { this.farmAddress = farmAddress; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
}
