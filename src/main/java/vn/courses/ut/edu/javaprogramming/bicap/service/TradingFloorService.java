package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.common.util.ImagesJson;
import vn.courses.ut.edu.javaprogramming.bicap.dto.CategoryResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.MarketplaceProductRegisterRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ProductListingResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Category;
import vn.courses.ut.edu.javaprogramming.bicap.entity.ExportStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Product;
import vn.courses.ut.edu.javaprogramming.bicap.entity.ProductStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.SeasonExport;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.CategoryRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingSeasonRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ProductRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.SeasonExportRepository;

import java.util.List;
import java.util.Set;

/**
 * Đăng ký đẩy sản phẩm đã xuất kho lên sàn giao dịch (BICAP-18 / SRS-FM-012).
 *
 * <p>Farm Manager chọn một lô hàng xuất kho (season_exports) đã có QR truy xuất
 * (trạng thái READY), bổ sung mô tả / số lượng / đơn giá dự kiến / ảnh / danh mục.
 * Hệ thống tạo bản ghi {@code products} với trạng thái {@code PENDING_REVIEW}
 * (chờ admin duyệt trước khi lên sàn cho Retailer đặt mua).
 */
@Service
@Transactional
@SuppressWarnings("null")
public class TradingFloorService {

    private static final int MAX_IMAGES = 10;

    private final ProductRepository productRepository;
    private final SeasonExportRepository exportRepository;
    private final FarmRepository farmRepository;
    private final CategoryRepository categoryRepository;
    private final FarmingSeasonRepository seasonRepository;
    private final LocalFileStorageService fileStorage;

    public TradingFloorService(ProductRepository productRepository,
                               SeasonExportRepository exportRepository,
                               FarmRepository farmRepository,
                               CategoryRepository categoryRepository,
                               FarmingSeasonRepository seasonRepository,
                               LocalFileStorageService fileStorage) {
        this.productRepository = productRepository;
        this.exportRepository = exportRepository;
        this.farmRepository = farmRepository;
        this.categoryRepository = categoryRepository;
        this.seasonRepository = seasonRepository;
        this.fileStorage = fileStorage;
    }

    /**
     * Tạo bản ghi sản phẩm chờ duyệt từ lô hàng xuất kho đã có QR.
     *
     * @param farmId  nông trại sở hữu lô hàng (phải thuộc Farm Manager đang đăng nhập)
     * @param request thông tin sản phẩm (exportId, name, description, quantity, price, categoryId)
     * @param images  1–10 ảnh sản phẩm dạng multipart
     */
    public ProductListingResponse registerProduct(Long farmId, MarketplaceProductRegisterRequest request,
                                                  List<MultipartFile> images) {
        User actor = requireFarmManager();
        Farm farm = requireOwnedFarm(farmId, actor.getId());

        if (images == null || images.isEmpty()) {
            throw new BadRequestException("At least one product image is required");
        }
        if (images.size() > MAX_IMAGES) {
            throw new BadRequestException("At most " + MAX_IMAGES + " product images are allowed");
        }

        // SRS-FM-012: chỉ đăng sản phẩm từ lô hàng ĐÃ xuất kho và có mã QR truy xuất hợp lệ.
        SeasonExport export = exportRepository.findById(request.exportId())
                .orElseThrow(() -> new ResourceNotFoundException("Export batch not found: " + request.exportId()));
        if (!export.getFarmId().equals(farm.getId())) {
            throw new ForbiddenException("Export batch does not belong to this farm");
        }
        if (export.getStatus() != ExportStatus.READY) {
            throw new BadRequestException("Only exports with a verified QR code can be listed on the trading floor");
        }
        if (request.quantity().compareTo(export.getQuantity()) > 0) {
            throw new BadRequestException("Listing quantity cannot exceed the exported quantity ("
                    + export.getQuantity() + " " + export.getUnit() + ")");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.categoryId()));

        List<String> urls = images.stream()
                .map(file -> fileStorage.storeProductImage(actor.getId(), file))
                .toList();

        Product product = Product.builder()
                .seasonId(export.getSeasonId())
                .exportId(export.getId())
                .categoryId(request.categoryId())
                .name(request.name().trim())
                .description(request.description().trim())
                .images(ImagesJson.toJson(urls))
                .price(request.price())
                .quantity(request.quantity().doubleValue())
                .status(ProductStatus.PENDING_REVIEW.name())
                .build();

        Product saved = productRepository.save(product);

        FarmingSeason season = seasonRepository.findById(export.getSeasonId()).orElse(null);
        return ProductListingResponse.fromEntity(saved, category, season, export);
    }

    /** Danh mục sản phẩm để form đăng ký đẩy lên sàn (BICAP-18). Cached — public read (BICAP-79). */
    @org.springframework.cache.annotation.Cacheable(vn.courses.ut.edu.javaprogramming.bicap.config.RedisCacheConfig.CACHE_CATEGORIES)
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::fromEntity)
                .toList();
    }

    /**
     * Danh sách sản phẩm nông trại đã đẩy lên sàn kèm trạng thái duyệt
     * (BICAP-19 / SRS-FM-013). Farm Manager chỉ xem được sản phẩm của chính mình.
     *
     * @param status bộ lọc tuỳ chọn: PENDING_REVIEW, ACTIVE, INACTIVE, REJECTED
     */
    @Transactional(readOnly = true)
    public List<ProductListingResponse> getFarmListings(Long farmId, String status) {
        User actor = requireFarmManager();
        Farm farm = requireOwnedFarm(farmId, actor.getId());

        String normalized = (status == null || status.isBlank()) ? null : status.trim().toUpperCase();
        return productRepository.findByFarmId(farm.getId(), normalized).stream()
                .map(p -> {
                    Category category = p.getCategoryId() != null
                            ? categoryRepository.findById(p.getCategoryId()).orElse(null) : null;
                    FarmingSeason season = p.getSeasonId() != null
                            ? seasonRepository.findById(p.getSeasonId()).orElse(null) : null;
                    SeasonExport export = p.getExportId() != null
                            ? exportRepository.findById(p.getExportId()).orElse(null) : null;
                    return ProductListingResponse.fromEntity(p, category, season, export);
                })
                .toList();
    }

    private User requireFarmManager() {
        User actor = CurrentUser.get();
        ActorAuthorizer.requireRoles(actor, Set.of("FARM_MANAGER"));
        return actor;
    }

    private Farm requireOwnedFarm(Long farmId, Long userId) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
        if (!farm.getUserId().equals(userId)) {
            throw new ForbiddenException("Farm does not belong to current user");
        }
        return farm;
    }
}
