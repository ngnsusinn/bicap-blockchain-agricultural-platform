package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload đăng ký đẩy sản phẩm lên sàn giao dịch (BICAP-18 / SRS-FM-012).
 * Được gửi dưới dạng JSON part ({@code request}) trong multipart request —
 * ảnh sản phẩm được gửi riêng qua các part {@code images}.
 *
 * @param exportId   Mã lô hàng xuất kho đã có QR truy xuất (bắt buộc)
 * @param name       Tên sản phẩm (không rỗng, tối đa 255 ký tự)
 * @param description Mô tả sản phẩm (tối thiểu 50, tối đa 2000 ký tự)
 * @param quantity   Số lượng bán (&gt; 0)
 * @param price      Đơn giá dự kiến VND (&gt; 0)
 * @param categoryId Danh mục sản phẩm (bắt buộc)
 */
public record MarketplaceProductRegisterRequest(
        @NotNull(message = "Export batch (exportId) is required")
        @Positive(message = "Export batch (exportId) must be positive")
        Long exportId,

        @NotBlank(message = "Product name is required")
        @Size(max = 255, message = "Product name must not exceed 255 characters")
        String name,

        @NotBlank(message = "Product description is required")
        @Size(min = 50, max = 2000, message = "Product description must be between 50 and 2000 characters")
        String description,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
        @Digits(integer = 16, fraction = 2, message = "Quantity has too many digits")
        BigDecimal quantity,

        @NotNull(message = "Expected price is required")
        @DecimalMin(value = "0.01", message = "Expected price must be greater than 0")
        @Digits(integer = 10, fraction = 2, message = "Price has too many digits")
        BigDecimal price,

        @NotNull(message = "Category is required")
        @Positive(message = "Category must be a positive id")
        Long categoryId) {}
