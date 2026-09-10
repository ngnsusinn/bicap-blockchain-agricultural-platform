package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.entity.Category;

import java.time.LocalDateTime;

/**
 * Product category summary (BICAP-5 / SRS-ADM-004).
 */
public class CategoryResponse {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private long productCount;
    private LocalDateTime createdAt;

    public CategoryResponse() {}

    public CategoryResponse(Long id, String name, String description, String icon,
                            long productCount, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.productCount = productCount;
        this.createdAt = createdAt;
    }

    public static CategoryResponse fromEntity(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription(),
                category.getIcon(), 0L, category.getCreatedAt());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public long getProductCount() { return productCount; }
    public void setProductCount(long productCount) { this.productCount = productCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
