package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class BlockchainWriteRequest {
    
    @NotBlank(message = "Dữ liệu (data) không được để trống")
    private String data;
    
    // Nếu có productId để link với data
    @NotNull(message = "ID sản phẩm (productId) không được để trống")
    private Long productId;

    public BlockchainWriteRequest() {
    }

    public BlockchainWriteRequest(String data, Long productId) {
        this.data = data;
        this.productId = productId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }
}
