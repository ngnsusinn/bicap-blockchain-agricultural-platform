package vn.courses.ut.edu.javaprogramming.bicap.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Product registered on the platform (BICAP-5 / SRS-ADM-004).
 * Maps to the `products` table. A product is created from an exported farming
 * season and carries traceability info (QR code) plus category and status.
 */
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "season_id", nullable = false)
    private Long seasonId;

    /** Nguồn lô hàng xuất kho (season_exports.id) khi sản phẩm được đăng lên sàn (BICAP-18 / SRS-FM-012). */
    @Column(name = "export_id")
    private Long exportId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    /** JSON array of uploaded product image URLs (BICAP-18 / SRS-FM-012: 1-10 ảnh). */
    @Column(columnDefinition = "TEXT")
    private String images;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Double quantity;

    @Column(name = "qr_code_id")
    private Long qrCodeId;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, PENDING_REVIEW

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Product() {
    }

    public Product(Long id, Long seasonId, Long exportId, Long categoryId, String name, String description,
                   String images, BigDecimal price, Double quantity, Long qrCodeId, String status,
                   LocalDateTime createdAt) {
        this.id = id;
        this.seasonId = seasonId;
        this.exportId = exportId;
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.images = images;
        this.price = price;
        this.quantity = quantity;
        this.qrCodeId = qrCodeId;
        this.status = status;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "ACTIVE";
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSeasonId() { return seasonId; }
    public void setSeasonId(Long seasonId) { this.seasonId = seasonId; }
    public Long getExportId() { return exportId; }
    public void setExportId(Long exportId) { this.exportId = exportId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public Long getQrCodeId() { return qrCodeId; }
    public void setQrCodeId(Long qrCodeId) { this.qrCodeId = qrCodeId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static ProductBuilder builder() {
        return new ProductBuilder();
    }

    public static class ProductBuilder {
        private Long id;
        private Long seasonId;
        private Long exportId;
        private Long categoryId;
        private String name;
        private String description;
        private String images;
        private BigDecimal price;
        private Double quantity;
        private Long qrCodeId;
        private String status;
        private LocalDateTime createdAt;

        ProductBuilder() {}

        public ProductBuilder id(Long id) { this.id = id; return this; }
        public ProductBuilder seasonId(Long seasonId) { this.seasonId = seasonId; return this; }
        public ProductBuilder exportId(Long exportId) { this.exportId = exportId; return this; }
        public ProductBuilder categoryId(Long categoryId) { this.categoryId = categoryId; return this; }
        public ProductBuilder name(String name) { this.name = name; return this; }
        public ProductBuilder description(String description) { this.description = description; return this; }
        public ProductBuilder images(String images) { this.images = images; return this; }
        public ProductBuilder price(BigDecimal price) { this.price = price; return this; }
        public ProductBuilder quantity(Double quantity) { this.quantity = quantity; return this; }
        public ProductBuilder qrCodeId(Long qrCodeId) { this.qrCodeId = qrCodeId; return this; }
        public ProductBuilder status(String status) { this.status = status; return this; }
        public ProductBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Product build() {
            return new Product(id, seasonId, exportId, categoryId, name, description,
                    images, price, quantity, qrCodeId, status, createdAt);
        }
    }
}
