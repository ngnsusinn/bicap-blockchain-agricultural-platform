package vn.courses.ut.edu.javaprogramming.bicap.service;

import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.common.util.SearchUtils;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ProductDetailResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ProductResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ProductStatsResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ProductStatusUpdateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Category;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Product;
import vn.courses.ut.edu.javaprogramming.bicap.entity.ProductStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.CategoryRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingSeasonRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ProductRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Product monitoring & management for admin (BICAP-5 / SRS-ADM-004):
 * - Paginated product list with category/status filters and search
 * - Dashboard statistics (totals per status, distribution per category, new this week)
 * - Product detail with season/farm/owner context
 * - Product status transitions (ACTIVE, INACTIVE, PENDING_REVIEW)
 */
@Service
@Transactional
@SuppressWarnings("null")
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final FarmingSeasonRepository seasonRepository;
    private final FarmRepository farmRepository;
    private final UserRepository userRepository;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          FarmingSeasonRepository seasonRepository,
                          FarmRepository farmRepository,
                          UserRepository userRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.seasonRepository = seasonRepository;
        this.farmRepository = farmRepository;
        this.userRepository = userRepository;
    }

    private void checkView(String actorEmail) {
        ActorAuthorizer.requireAdminView(userRepository, actorEmail);
    }

    private void checkWrite(String actorEmail) {
        ActorAuthorizer.requireAdminWrite(userRepository, actorEmail);
    }

    // ── Read operations ──

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(String status, Long categoryId, String search,
                                             Pageable pageable, String actorEmail) {
        checkView(actorEmail);
        Page<Product> products = productRepository.findProductsFiltered(
                status, categoryId, SearchUtils.escapeLike(search), pageable);

        List<Product> content = products.getContent();
        if (content.isEmpty()) {
            return Page.empty(products.getPageable());
        }

        // Batch-load lookup entities for the whole page to avoid N+1 queries
        // (one category/season/farm query per product would otherwise run for each row).
        Map<Long, Category> categories = categoryRepository.findAllById(
                        content.stream().map(Product::getCategoryId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Category::getId, c -> c));

        Map<Long, FarmingSeason> seasons = seasonRepository.findAllById(
                        content.stream().map(Product::getSeasonId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(FarmingSeason::getId, s -> s));

        Set<Long> farmIds = seasons.values().stream()
                .map(FarmingSeason::getFarmId).collect(Collectors.toSet());
        Map<Long, Farm> farms = farmIds.isEmpty() ? Map.of() : farmRepository.findAllById(farmIds)
                .stream().collect(Collectors.toMap(Farm::getId, f -> f));

        return products.map(product -> {
            FarmingSeason season = seasons.get(product.getSeasonId());
            return ProductResponse.fromEntity(product,
                    categories.get(product.getCategoryId()),
                    season,
                    season != null ? farms.get(season.getFarmId()) : null);
        });
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long id, String actorEmail) {
        checkView(actorEmail);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        Category category = categoryRepository.findById(product.getCategoryId()).orElse(null);
        FarmingSeason season = seasonRepository.findById(product.getSeasonId()).orElse(null);
        Farm farm = season != null ? farmRepository.findById(season.getFarmId()).orElse(null) : null;
        User owner = farm != null ? userRepository.findById(farm.getUserId()).orElse(null) : null;

        return ProductDetailResponse.fromEntity(product, category, season, farm, owner);
    }

    @Transactional(readOnly = true)
    public ProductStatsResponse getProductStats(String actorEmail) {
        checkView(actorEmail);
        long total = productRepository.count();
        long active = productRepository.countByStatus(ProductStatus.ACTIVE.name());
        long inactive = productRepository.countByStatus(ProductStatus.INACTIVE.name());
        long pendingReview = productRepository.countByStatus(ProductStatus.PENDING_REVIEW.name());
        long newThisWeek = productRepository.countNewSince(LocalDateTime.now().minusDays(7));

        Map<Long, Long> counts = productRepository.countByCategory().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        Map<Long, Category> categories = categoryRepository.findAllById(counts.keySet())
                .stream().collect(Collectors.toMap(Category::getId, c -> c));

        List<ProductStatsResponse.CategoryStat> byCategory = counts.entrySet().stream()
                .map(entry -> new ProductStatsResponse.CategoryStat(
                        entry.getKey(),
                        categories.get(entry.getKey()) != null ? categories.get(entry.getKey()).getName() : null,
                        entry.getValue()))
                .collect(Collectors.toList());

        return new ProductStatsResponse(total, active, inactive, pendingReview, newThisWeek, byCategory);
    }

    // ── Mutations ──

    public ProductResponse updateStatus(Long id, ProductStatusUpdateRequest request, String actorEmail) {
        checkWrite(actorEmail);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        ProductStatus newStatus;
        try {
            newStatus = ProductStatus.valueOf(request.getStatus());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid product status: " + request.getStatus());
        }

        if (!product.getStatus().equals(newStatus.name())) {
            product.setStatus(newStatus.name());
            productRepository.save(product);
        }

        Category category = categoryRepository.findById(product.getCategoryId()).orElse(null);
        FarmingSeason season = seasonRepository.findById(product.getSeasonId()).orElse(null);
        Farm farm = season != null ? farmRepository.findById(season.getFarmId()).orElse(null) : null;
        return ProductResponse.fromEntity(product, category, season, farm);
    }
}
