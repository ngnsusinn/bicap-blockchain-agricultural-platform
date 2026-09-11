package vn.courses.ut.edu.javaprogramming.bicap.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.dto.MarketplaceProductResponse;
import vn.courses.ut.edu.javaprogramming.bicap.service.MarketplaceService;

import java.math.BigDecimal;
import java.util.List;

/**
 * Public (guest) catalogue — F3 / C-3 fix.
 *
 * <p>Replaces the previous guest implementation that called the admin monitoring
 * endpoint {@code /api/admin/products} and then fabricated {@code traceHash},
 * certification, unit and image values in the browser. This endpoint returns the
 * authoritative data (category, farm, farm address as origin, real certifications,
 * real export trace hash, real images, availability) for ACTIVE products only.
 *
 * <p>Permitted anonymously through {@code /api/public/**} in {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/public/products")
public class PublicCatalogController {

    private final MarketplaceService marketplace;

    public PublicCatalogController(MarketplaceService marketplace) {
        this.marketplace = marketplace;
    }

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
        return ResponseEntity.ok(marketplace.searchPublic(keyword, categoryId, region, certification,
                minPrice, maxPrice, availability, sortBy, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MarketplaceProductResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(marketplace.detailPublic(id));
    }

    @GetMapping("/trace/{hash}")
    public ResponseEntity<MarketplaceProductResponse> trace(@PathVariable String hash) {
        return ResponseEntity.ok(marketplace.trace(hash));
    }
}
