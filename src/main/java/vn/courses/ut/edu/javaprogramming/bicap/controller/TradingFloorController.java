package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.courses.ut.edu.javaprogramming.bicap.dto.CategoryResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.MarketplaceProductRegisterRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ProductListingResponse;
import vn.courses.ut.edu.javaprogramming.bicap.service.TradingFloorService;

import java.util.List;

/**
 * Sàn giao dịch — phía Farm Manager (BICAP-18 / SRS-FM-012).
 *
 * <p>{@code POST /api/farms/{farmId}/marketplace/products}: đăng ký đẩy sản phẩm
 * đã xuất kho lên sàn. Request là multipart gồm part {@code request} (JSON) chứa
 * thông tin sản phẩm và các part {@code images} (1–10 file ảnh).
 */
@RestController
@RequestMapping("/api")
public class TradingFloorController {

    private final TradingFloorService tradingFloorService;

    public TradingFloorController(TradingFloorService tradingFloorService) {
        this.tradingFloorService = tradingFloorService;
    }

    @PostMapping(value = "/farms/{farmId}/marketplace/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductListingResponse> registerProduct(
            @PathVariable Long farmId,
            @RequestPart("request") @Valid MarketplaceProductRegisterRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        ProductListingResponse created = tradingFloorService.registerProduct(farmId, request, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Danh mục sản phẩm cho form đăng ký đẩy lên sàn (BICAP-18). */
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        return ResponseEntity.ok(tradingFloorService.getCategories());
    }
}
