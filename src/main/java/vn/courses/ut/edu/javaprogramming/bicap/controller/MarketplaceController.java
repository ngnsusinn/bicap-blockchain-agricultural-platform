package vn.courses.ut.edu.javaprogramming.bicap.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.dto.MarketplaceProductResponse;
import vn.courses.ut.edu.javaprogramming.bicap.service.MarketplaceService;

import java.math.BigDecimal;
import java.util.List;

/** Retailer marketplace endpoints for BICAP-39/40/41. */
@RestController
@RequestMapping("/api/marketplace/products")
public class MarketplaceController {
    private final MarketplaceService marketplace;
    public MarketplaceController(MarketplaceService marketplace) { this.marketplace = marketplace; }

    @GetMapping
    public ResponseEntity<Page<MarketplaceProductResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) List<String> certification,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String availability,
            @RequestParam(defaultValue = "NEWEST") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(marketplace.search(keyword, categoryId, region, certification,
                minPrice, maxPrice, availability, sortBy, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MarketplaceProductResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(marketplace.detail(id));
    }

    @GetMapping("/trace/{hash}")
    public ResponseEntity<MarketplaceProductResponse> trace(@PathVariable String hash) {
        return ResponseEntity.ok(marketplace.trace(hash));
    }
}
