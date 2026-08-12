package vn.courses.ut.edu.javaprogramming.bicap.controller;

import vn.courses.ut.edu.javaprogramming.bicap.dto.CategoryRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.CategoryResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ProductDetailResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ProductResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ProductStatsResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ProductStatusUpdateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.service.CategoryService;
import vn.courses.ut.edu.javaprogramming.bicap.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin endpoints for product monitoring & category management (BICAP-5 / SRS-ADM-004).
 * Static paths (/stats, /categories) are declared before the dynamic /{id} so Spring
 * matches the literal segments first.
 */
@RestController
@RequestMapping("/api/admin/products")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public ProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    // ── Product monitoring ──

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @RequestHeader("X-Actor-Email") String actorEmail,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ProductResponse> products = productService.getProducts(status, categoryId, search, pageable, actorEmail);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/stats")
    public ResponseEntity<ProductStatsResponse> getStats(@RequestHeader("X-Actor-Email") String actorEmail) {
        return ResponseEntity.ok(productService.getProductStats(actorEmail));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> getProductDetail(
            @RequestHeader("X-Actor-Email") String actorEmail,
            @PathVariable Long id) {
        ProductDetailResponse product = productService.getProductDetail(id, actorEmail);
        return ResponseEntity.ok(product);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ProductResponse> updateStatus(
            @RequestHeader("X-Actor-Email") String actorEmail,
            @PathVariable Long id,
            @Valid @RequestBody ProductStatusUpdateRequest request) {
        ProductResponse updated = productService.updateStatus(id, request, actorEmail);
        return ResponseEntity.ok(updated);
    }

    // ── Category management ──

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories(
            @RequestHeader("X-Actor-Email") String actorEmail) {
        return ResponseEntity.ok(categoryService.getAllCategories(actorEmail));
    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<CategoryResponse> getCategory(
            @RequestHeader("X-Actor-Email") String actorEmail,
            @PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id, actorEmail));
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(
            @RequestHeader("X-Actor-Email") String actorEmail,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = categoryService.createCategory(request, actorEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @RequestHeader("X-Actor-Email") String actorEmail,
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse updated = categoryService.updateCategory(id, request, actorEmail);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(
            @RequestHeader("X-Actor-Email") String actorEmail,
            @PathVariable Long id) {
        categoryService.deleteCategory(id, actorEmail);
        return ResponseEntity.noContent().build();
    }
}
