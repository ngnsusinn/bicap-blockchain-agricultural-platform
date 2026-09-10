package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.entity.BusinessType;
import vn.courses.ut.edu.javaprogramming.bicap.entity.RetailerBusinessProfile;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Thông tin tóm tắt một Nhà bán lẻ đã có giao dịch (hợp đồng) với nông trại của
 * Farm Manager (BICAP-21 / SRS-FM-015).
 *
 * <p>Kết hợp thông tin cá nhân Retailer (User) và hồ sơ kinh doanh
 * (RetailerBusinessProfile: tên doanh nghiệp, giấy phép kinh doanh) cùng các chỉ số
 * giao dịch tổng hợp từ đơn hàng: số đơn, tổng giá trị, lần giao dịch đầu/cuối.
 * Dùng cho danh sách đối tác.
 */
public class RetailerPartnerResponse {
    private Long retailerId;
    private String retailerName;
    private String retailerEmail;
    private String retailerPhone;
    private String retailerAddress;

    private String businessName;
    private String businessAddress;
    private BusinessType businessType;
    private String licenseUrl;

    private long totalOrders;
    private BigDecimal totalSpent;
    private LocalDateTime firstOrderAt;
    private LocalDateTime lastOrderAt;

    public RetailerPartnerResponse() {
    }

    public RetailerPartnerResponse(Long retailerId, String retailerName, String retailerEmail,
                                   String retailerPhone, String retailerAddress, String businessName,
                                   String businessAddress, BusinessType businessType, String licenseUrl,
                                   long totalOrders, BigDecimal totalSpent, LocalDateTime firstOrderAt,
                                   LocalDateTime lastOrderAt) {
        this.retailerId = retailerId;
        this.retailerName = retailerName;
        this.retailerEmail = retailerEmail;
        this.retailerPhone = retailerPhone;
        this.retailerAddress = retailerAddress;
        this.businessName = businessName;
        this.businessAddress = businessAddress;
        this.businessType = businessType;
        this.licenseUrl = licenseUrl;
        this.totalOrders = totalOrders;
        this.totalSpent = totalSpent;
        this.firstOrderAt = firstOrderAt;
        this.lastOrderAt = lastOrderAt;
    }

    /** Builds the response from the retailer user and business profile (both may be null-safe). */
    public static RetailerPartnerResponse from(Long retailerId, User retailer, RetailerBusinessProfile profile,
                                               long totalOrders, BigDecimal totalSpent,
                                               LocalDateTime firstOrderAt, LocalDateTime lastOrderAt) {
        return new RetailerPartnerResponse(
                retailerId,
                retailer != null ? retailer.getFullName() : null,
                retailer != null ? retailer.getEmail() : null,
                retailer != null ? retailer.getPhone() : null,
                retailer != null ? retailer.getAddress() : null,
                profile != null ? profile.getBusinessName() : null,
                profile != null ? profile.getAddress() : null,
                profile != null ? profile.getBusinessType() : null,
                profile != null ? profile.getLicenseUrl() : null,
                totalOrders,
                totalSpent,
                firstOrderAt,
                lastOrderAt
        );
    }

    public Long getRetailerId() { return retailerId; }
    public void setRetailerId(Long retailerId) { this.retailerId = retailerId; }
    public String getRetailerName() { return retailerName; }
    public void setRetailerName(String retailerName) { this.retailerName = retailerName; }
    public String getRetailerEmail() { return retailerEmail; }
    public void setRetailerEmail(String retailerEmail) { this.retailerEmail = retailerEmail; }
    public String getRetailerPhone() { return retailerPhone; }
    public void setRetailerPhone(String retailerPhone) { this.retailerPhone = retailerPhone; }
    public String getRetailerAddress() { return retailerAddress; }
    public void setRetailerAddress(String retailerAddress) { this.retailerAddress = retailerAddress; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getBusinessAddress() { return businessAddress; }
    public void setBusinessAddress(String businessAddress) { this.businessAddress = businessAddress; }
    public BusinessType getBusinessType() { return businessType; }
    public void setBusinessType(BusinessType businessType) { this.businessType = businessType; }
    public String getLicenseUrl() { return licenseUrl; }
    public void setLicenseUrl(String licenseUrl) { this.licenseUrl = licenseUrl; }
    public long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }
    public BigDecimal getTotalSpent() { return totalSpent; }
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }
    public LocalDateTime getFirstOrderAt() { return firstOrderAt; }
    public void setFirstOrderAt(LocalDateTime firstOrderAt) { this.firstOrderAt = firstOrderAt; }
    public LocalDateTime getLastOrderAt() { return lastOrderAt; }
    public void setLastOrderAt(LocalDateTime lastOrderAt) { this.lastOrderAt = lastOrderAt; }
}
