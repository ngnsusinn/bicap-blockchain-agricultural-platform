package vn.courses.ut.edu.javaprogramming.bicap.dto;

import java.util.List;

/**
 * Dashboard statistics for the product monitoring page (BICAP-5 / SRS-ADM-004):
 * totals per status, distribution per category and new products this week.
 */
public class ProductStatsResponse {
    private long totalProducts;
    private long activeProducts;
    private long inactiveProducts;
    private long pendingReviewProducts;
    private long newProductsThisWeek;
    private List<CategoryStat> byCategory;

    public ProductStatsResponse() {}

    public ProductStatsResponse(long totalProducts, long activeProducts, long inactiveProducts,
                                long pendingReviewProducts, long newProductsThisWeek,
                                List<CategoryStat> byCategory) {
        this.totalProducts = totalProducts;
        this.activeProducts = activeProducts;
        this.inactiveProducts = inactiveProducts;
        this.pendingReviewProducts = pendingReviewProducts;
        this.newProductsThisWeek = newProductsThisWeek;
        this.byCategory = byCategory;
    }

    public long getTotalProducts() { return totalProducts; }
    public void setTotalProducts(long totalProducts) { this.totalProducts = totalProducts; }
    public long getActiveProducts() { return activeProducts; }
    public void setActiveProducts(long activeProducts) { this.activeProducts = activeProducts; }
    public long getInactiveProducts() { return inactiveProducts; }
    public void setInactiveProducts(long inactiveProducts) { this.inactiveProducts = inactiveProducts; }
    public long getPendingReviewProducts() { return pendingReviewProducts; }
    public void setPendingReviewProducts(long pendingReviewProducts) { this.pendingReviewProducts = pendingReviewProducts; }
    public long getNewProductsThisWeek() { return newProductsThisWeek; }
    public void setNewProductsThisWeek(long newProductsThisWeek) { this.newProductsThisWeek = newProductsThisWeek; }
    public List<CategoryStat> getByCategory() { return byCategory; }
    public void setByCategory(List<CategoryStat> byCategory) { this.byCategory = byCategory; }

    /** One row of the "products per category" distribution. */
    public static class CategoryStat {
        private Long categoryId;
        private String categoryName;
        private long count;

        public CategoryStat() {}

        public CategoryStat(Long categoryId, String categoryName, long count) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.count = count;
        }

        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }
}
