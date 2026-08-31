package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.common.util.ImagesJson;
import vn.courses.ut.edu.javaprogramming.bicap.dto.MarketplaceProductResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.*;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.*;

import java.math.BigDecimal;
import java.util.*;

/** Read-only Retailer marketplace composed from the existing farm/export/product modules. */
@Service
@Transactional(readOnly = true)
public class MarketplaceService {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final FarmingSeasonRepository seasons;
    private final FarmRepository farms;
    private final FarmCertificationRepository certifications;
    private final SeasonExportRepository exports;
    private final FarmingProcessRepository processes;

    public MarketplaceService(ProductRepository products, CategoryRepository categories,
                              FarmingSeasonRepository seasons, FarmRepository farms,
                              FarmCertificationRepository certifications, SeasonExportRepository exports,
                              FarmingProcessRepository processes) {
        this.products = products; this.categories = categories; this.seasons = seasons; this.farms = farms;
        this.certifications = certifications; this.exports = exports; this.processes = processes;
    }

    public Page<MarketplaceProductResponse> search(String keyword, Long categoryId, String region,
                                                    List<String> certification, BigDecimal minPrice,
                                                    BigDecimal maxPrice, String availability,
                                                    String sortBy, int page, int size) {
        requireRetailer();
        if (page < 0 || size < 1 || size > 100) throw new BadRequestException("Invalid pagination");
        if (minPrice != null && minPrice.signum() < 0) throw new BadRequestException("minPrice must be at least 0");
        if (maxPrice != null && (maxPrice.signum() < 0 || minPrice != null && maxPrice.compareTo(minPrice) <= 0))
            throw new BadRequestException("maxPrice must be greater than minPrice");

        String q = normalize(keyword), r = normalize(region);
        Set<String> certs = new HashSet<>((certification == null ? List.<String>of() : certification)
                .stream().map(MarketplaceService::normalize).filter(Objects::nonNull).toList());

        List<MarketplaceProductResponse> matches = products.findAll().stream()
                .filter(p -> "ACTIVE".equals(p.getStatus()))
                .map(p -> build(p, false))
                .filter(p -> q == null || contains(p.name(), q) || contains(p.farmName(), q))
                .filter(p -> categoryId == null || categoryId.equals(p.categoryId()))
                .filter(p -> r == null || contains(p.farmAddress(), r))
                .filter(p -> certs.isEmpty() || p.certifications().stream().map(MarketplaceService::normalize).anyMatch(certs::contains))
                .filter(p -> minPrice == null || p.price().compareTo(minPrice) >= 0)
                .filter(p -> maxPrice == null || p.price().compareTo(maxPrice) <= 0)
                .filter(p -> availability == null || availability.isBlank() || availability.equalsIgnoreCase(p.availability()))
                .sorted(comparator(sortBy))
                .toList();
        int from = Math.min(page * size, matches.size());
        int to = Math.min(from + size, matches.size());
        return new PageImpl<>(matches.subList(from, to), PageRequest.of(page, size), matches.size());
    }

    @org.springframework.cache.annotation.Cacheable(cacheNames =
            vn.courses.ut.edu.javaprogramming.bicap.config.RedisCacheConfig.CACHE_MARKETPLACE_DETAIL, key = "#id")
    public MarketplaceProductResponse detail(Long id) {
        requireRetailer();
        Product product = products.findById(id).filter(p -> "ACTIVE".equals(p.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace product not found"));
        return build(product, true);
    }

    public MarketplaceProductResponse trace(String traceHash) {
        requireRetailer();
        SeasonExport export = exports.findByTraceHash(traceHash)
                .orElseThrow(() -> new ResourceNotFoundException("Traceable export not found"));
        Product product = products.findByExportId(export.getId())
                .filter(p -> "ACTIVE".equals(p.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace product not found for this QR code"));
        return build(product, true);
    }

    private MarketplaceProductResponse build(Product product, boolean includeProcesses) {
        Category category = categories.findById(product.getCategoryId()).orElse(null);
        FarmingSeason season = seasons.findById(product.getSeasonId()).orElse(null);
        Farm farm = season == null ? null : farms.findById(season.getFarmId()).orElse(null);
        SeasonExport export = product.getExportId() == null ? null : exports.findById(product.getExportId()).orElse(null);
        List<String> certs = farm == null ? List.of() : certifications.findByFarmId(farm.getId()).stream()
                .map(FarmCertification::getType).distinct().toList();
        List<MarketplaceProductResponse.TraceProcess> timeline = !includeProcesses || season == null ? List.of() :
                processes.findBySeasonId(season.getId()).stream()
                        .sorted(Comparator.comparing(FarmingProcess::getExecutionDate))
                        .map(p -> new MarketplaceProductResponse.TraceProcess(p.getProcessType(), p.getExecutionDate(),
                                p.getMaterials(), p.getImages(), p.getNotes(), p.getTxHash())).toList();
        return new MarketplaceProductResponse(product.getId(), product.getName(), product.getDescription(),
                ImagesJson.parse(product.getImages()), product.getPrice(), product.getQuantity(),
                product.getQuantity() != null && product.getQuantity() > 0 ? "AVAILABLE" : "SOLD_OUT",
                product.getCategoryId(), category == null ? null : category.getName(),
                farm == null ? null : farm.getId(), farm == null ? null : farm.getName(), farm == null ? null : farm.getAddress(),
                certs, season == null ? null : season.getId(), season == null ? null : season.getName(),
                season == null ? null : season.getProductType(), season == null ? null : season.getVariety(),
                season == null ? null : season.getStartDate(), season == null ? null : season.getEndDate(),
                export == null ? null : export.getId(), export == null ? null : export.getTraceHash(),
                export == null ? null : export.getQrImage(), export == null ? null : export.getTransactionHash(),
                timeline, product.getCreatedAt());
    }

    private static Comparator<MarketplaceProductResponse> comparator(String sortBy) {
        return switch (sortBy == null ? "NEWEST" : sortBy.toUpperCase()) {
            case "PRICE_ASC" -> Comparator.comparing(MarketplaceProductResponse::price);
            case "PRICE_DESC" -> Comparator.comparing(MarketplaceProductResponse::price).reversed();
            default -> Comparator.comparing(MarketplaceProductResponse::createdAt,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        };
    }
    private static String normalize(String value) { return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT); }
    private static boolean contains(String value, String q) { return value != null && value.toLowerCase(Locale.ROOT).contains(q); }
    private static void requireRetailer() { ActorAuthorizer.requireRoles(CurrentUser.get(), Set.of("RETAILER")); }
}
