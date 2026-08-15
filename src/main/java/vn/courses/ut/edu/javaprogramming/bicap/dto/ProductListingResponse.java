package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.common.util.ImagesJson;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Category;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Product;
import vn.courses.ut.edu.javaprogramming.bicap.entity.SeasonExport;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Payload trả về sau khi Farm Manager đăng ký đẩy sản phẩm lên sàn giao dịch
 * (BICAP-18 / SRS-FM-012). Kế thừa thông tin sản phẩm và bổ sung nguồn lô xuất kho
 * (mã QR truy xuất) để giao diện hiển thị trạng thái chờ duyệt.
 */
public class ProductListingResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Double quantity;
    private Long categoryId;
    private String categoryName;
    private Long seasonId;
    private String seasonName;
    private Long exportId;
    private String traceHash;
    private String qrImage;
    private List<String> images;
    private String status;
    private LocalDateTime createdAt;

    public ProductListingResponse() {}

    public ProductListingResponse(Long id, String name, String description, BigDecimal price, Double quantity,
                                  Long categoryId, String categoryName, Long seasonId, String seasonName,
                                  Long exportId, String traceHash, String qrImage, List<String> images,
                                  String status, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.seasonId = seasonId;
        this.seasonName = seasonName;
        this.exportId = exportId;
        this.traceHash = traceHash;
        this.qrImage = qrImage;
        this.images = images;
        this.status = status;
        this.createdAt = createdAt;
    }

    /** Builds the response from the saved product plus its category/season/export lookups. */
    public static ProductListingResponse fromEntity(Product product, Category category, FarmingSeason season, SeasonExport export) {
        return new ProductListingResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantity(),
                product.getCategoryId(),
                category != null ? category.getName() : null,
                product.getSeasonId(),
                season != null ? season.getName() : null,
                export != null ? export.getId() : null,
                export != null ? export.getTraceHash() : null,
                export != null ? export.getQrImage() : null,
                ImagesJson.parse(product.getImages()),
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
    public Long getExportId() { return exportId; }
    public void setExportId(Long exportId) { this.exportId = exportId; }
    public String getTraceHash() { return traceHash; }
    public void setTraceHash(String traceHash) { this.traceHash = traceHash; }
    public String getQrImage() { return qrImage; }
    public void setQrImage(String qrImage) { this.qrImage = qrImage; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
