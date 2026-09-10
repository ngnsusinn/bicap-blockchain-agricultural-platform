package vn.courses.ut.edu.javaprogramming.bicap;

import vn.courses.ut.edu.javaprogramming.bicap.dto.CategoryRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.CategoryResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ProductDetailResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ProductResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ProductStatusUpdateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Category;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Product;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Role;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ConflictException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.CategoryRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingSeasonRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ProductRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.CategoryService;
import vn.courses.ut.edu.javaprogramming.bicap.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unused", "null"})
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private FarmingSeasonRepository seasonRepository;
    @Mock
    private FarmRepository farmRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProductService productService;

    @InjectMocks
    private CategoryService categoryService;

    private User superAdmin;
    private User farmer;
    private User retailer;
    private Farm farm;
    private FarmingSeason season;
    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {
        Role superAdminRole = Role.builder().id(1L).name("SUPER_ADMIN").permissions(new HashSet<>()).build();
        Role farmRole = Role.builder().id(4L).name("FARM_MANAGER").permissions(new HashSet<>()).build();
        Role retailRole = Role.builder().id(5L).name("RETAILER").permissions(new HashSet<>()).build();

        superAdmin = User.builder()
                .id(1L).email("super@bicap.com").password("Secret@2026")
                .fullName("Super Admin").status(UserStatus.ACTIVE).roles(Set.of(superAdminRole))
                .build();
        farmer = User.builder()
                .id(10L).email("farmer@bicap.com").password("Secret@2026")
                .fullName("Chủ Trang Trại").status(UserStatus.ACTIVE).roles(Set.of(farmRole))
                .build();
        retailer = User.builder()
                .id(20L).email("retailer@bicap.com").password("Secret@2026")
                .fullName("Nhà Bán Lẻ").status(UserStatus.ACTIVE).roles(Set.of(retailRole))
                .build();

        farm = Farm.builder()
                .id(100L).userId(10L).name("Trang Trại Xanh")
                .address("Đồng Nai").area(12.5)
                .status(FarmStatus.APPROVED)
                .build();
        season = new FarmingSeason(200L, 100L, "Mùa Rau Xuân", "Rau ăn lá", "Cải xanh",
                2.0, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1),
                "HARVESTED", "0xabc", null);
        category = Category.builder()
                .id(300L).name("Rau ăn lá").description("Rau gia vị").icon("🥬")
                .build();
        product = Product.builder()
                .id(400L).seasonId(200L).categoryId(300L).name("Cải xanh hữu cơ")
                .description("Rau cải xanh VietGAP").price(new BigDecimal("15000"))
                .quantity(500.0).status("ACTIVE")
                .build();
    }

    // ── getProducts ──

    @Test
    void getProducts_shouldBatchLoadLookups() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1);
        when(productRepository.findProductsFiltered(any(), any(), any(), any())).thenReturn(page);
        when(categoryRepository.findAllById(any())).thenReturn(List.of(category));
        when(seasonRepository.findAllById(any())).thenReturn(List.of(season));
        when(farmRepository.findAllById(any())).thenReturn(List.of(farm));

        Page<ProductResponse> result = productService.getProducts(null, null, null,
                PageRequest.of(0, 10), "super@bicap.com");

        assertEquals(1, result.getTotalElements());
        ProductResponse resp = result.getContent().get(0);
        assertEquals("Cải xanh hữu cơ", resp.getName());
        assertEquals("Rau ăn lá", resp.getCategoryName());
        assertEquals("Mùa Rau Xuân", resp.getSeasonName());
        assertEquals("Trang Trại Xanh", resp.getFarmName());
        assertEquals("ACTIVE", resp.getStatus());

        // Batch-loading — one query per lookup table, never per-row
        verify(categoryRepository, times(1)).findAllById(any());
        verify(seasonRepository, times(1)).findAllById(any());
        verify(farmRepository, times(1)).findAllById(any());
        verify(categoryRepository, never()).findById(anyLong());
        verify(seasonRepository, never()).findById(anyLong());
    }

    @Test
    void getProducts_byNonAdmin_shouldThrowForbidden() {
        when(userRepository.findByEmail("retailer@bicap.com")).thenReturn(Optional.of(retailer));

        assertThrows(ForbiddenException.class,
                () -> productService.getProducts(null, null, null, PageRequest.of(0, 10), "retailer@bicap.com"));
        verify(productRepository, never()).findProductsFiltered(any(), any(), any(), any());
    }

    @Test
    void getProducts_emptyPage_shouldNotCallLookups() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        Page<Product> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(productRepository.findProductsFiltered(any(), any(), any(), any())).thenReturn(page);

        Page<ProductResponse> result = productService.getProducts(null, null, "không-có",
                PageRequest.of(0, 10), "super@bicap.com");

        assertEquals(0, result.getTotalElements());
        verify(categoryRepository, never()).findAllById(any());
        verify(seasonRepository, never()).findAllById(any());
    }

    // ── getProductDetail ──

    @Test
    void getProductDetail_shouldIncludeSeasonFarmOwner() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(productRepository.findById(400L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(300L)).thenReturn(Optional.of(category));
        when(seasonRepository.findById(200L)).thenReturn(Optional.of(season));
        when(farmRepository.findById(100L)).thenReturn(Optional.of(farm));
        when(userRepository.findById(10L)).thenReturn(Optional.of(farmer));

        ProductDetailResponse detail = productService.getProductDetail(400L, "super@bicap.com");

        assertEquals("Cải xanh hữu cơ", detail.getName());
        assertEquals("Trang Trại Xanh", detail.getFarmName());
        assertEquals("Chủ Trang Trại", detail.getOwnerName());
        assertEquals("Đồng Nai", detail.getFarmAddress());
        assertEquals("Cải xanh", detail.getSeasonVariety());
        assertEquals(LocalDate.of(2026, 1, 1), detail.getSeasonStartDate());
    }

    @Test
    void getProductDetail_unknownProduct_shouldThrowNotFound() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.getProductDetail(999L, "super@bicap.com"));
    }

    // ── updateStatus ──

    @Test
    void updateStatus_shouldChangeProductStatus() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(productRepository.findById(400L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoryRepository.findById(300L)).thenReturn(Optional.of(category));
        when(seasonRepository.findById(200L)).thenReturn(Optional.of(season));
        when(farmRepository.findById(100L)).thenReturn(Optional.of(farm));

        ProductStatusUpdateRequest request = new ProductStatusUpdateRequest("PENDING_REVIEW");
        ProductResponse result = productService.updateStatus(400L, request, "super@bicap.com");

        assertEquals("PENDING_REVIEW", result.getStatus());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateStatus_sameStatus_shouldBeIdempotent() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(productRepository.findById(400L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(300L)).thenReturn(Optional.of(category));
        when(seasonRepository.findById(200L)).thenReturn(Optional.of(season));
        when(farmRepository.findById(100L)).thenReturn(Optional.of(farm));

        ProductStatusUpdateRequest request = new ProductStatusUpdateRequest("ACTIVE");
        ProductResponse result = productService.updateStatus(400L, request, "super@bicap.com");

        assertEquals("ACTIVE", result.getStatus());
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateStatus_invalidStatus_shouldThrowBadRequest() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(productRepository.findById(400L)).thenReturn(Optional.of(product));

        ProductStatusUpdateRequest request = new ProductStatusUpdateRequest("DELETED");
        assertThrows(BadRequestException.class,
                () -> productService.updateStatus(400L, request, "super@bicap.com"));
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateStatus_byNonAdmin_shouldThrowForbidden() {
        when(userRepository.findByEmail("retailer@bicap.com")).thenReturn(Optional.of(retailer));

        ProductStatusUpdateRequest request = new ProductStatusUpdateRequest("INACTIVE");
        assertThrows(ForbiddenException.class,
                () -> productService.updateStatus(400L, request, "retailer@bicap.com"));
        verify(productRepository, never()).findById(anyLong());
    }

    // ── getProductStats ──

    @Test
    void getProductStats_shouldReturnCountsAndDistribution() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(productRepository.count()).thenReturn(5L);
        when(productRepository.countByStatus("ACTIVE")).thenReturn(3L);
        when(productRepository.countByStatus("INACTIVE")).thenReturn(1L);
        when(productRepository.countByStatus("PENDING_REVIEW")).thenReturn(1L);
        when(productRepository.countNewSince(any())).thenReturn(2L);
        when(productRepository.countByCategory()).thenReturn(List.<Object[]>of(new Object[]{300L, 5L}));
        when(categoryRepository.findAllById(any())).thenReturn(List.of(category));

        var stats = productService.getProductStats("super@bicap.com");

        assertEquals(5L, stats.getTotalProducts());
        assertEquals(3L, stats.getActiveProducts());
        assertEquals(1L, stats.getInactiveProducts());
        assertEquals(1L, stats.getPendingReviewProducts());
        assertEquals(2L, stats.getNewProductsThisWeek());
        assertEquals(1, stats.getByCategory().size());
        assertEquals("Rau ăn lá", stats.getByCategory().get(0).getCategoryName());
        assertEquals(5L, stats.getByCategory().get(0).getCount());
    }

    // ── CategoryService ──

    @Test
    void createCategory_duplicateName_shouldThrowConflict() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(categoryRepository.existsByNameIgnoreCase("Rau ăn lá")).thenReturn(true);

        CategoryRequest request = new CategoryRequest("Rau ăn lá", "Mô tả", "🥬");
        assertThrows(ConflictException.class,
                () -> categoryService.createCategory(request, "super@bicap.com"));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createCategory_byNonAdmin_shouldThrowForbidden() {
        when(userRepository.findByEmail("retailer@bicap.com")).thenReturn(Optional.of(retailer));

        CategoryRequest request = new CategoryRequest("Trái cây", "Mô tả", "🍎");
        assertThrows(ForbiddenException.class,
                () -> categoryService.createCategory(request, "retailer@bicap.com"));
    }

    @Test
    void createCategory_shouldSaveAndReturn() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(categoryRepository.existsByNameIgnoreCase("Trái cây")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(500L);
            return c;
        });

        CategoryRequest request = new CategoryRequest("Trái cây", "Các loại trái cây", "🍎");
        CategoryResponse result = categoryService.createCategory(request, "super@bicap.com");

        assertEquals("Trái cây", result.getName());
        assertEquals("Các loại trái cây", result.getDescription());
        assertEquals(0L, result.getProductCount());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void deleteCategory_withProducts_shouldThrowBadRequest() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(categoryRepository.findById(300L)).thenReturn(Optional.of(category));
        when(productRepository.countByCategory()).thenReturn(List.<Object[]>of(new Object[]{300L, 2L}));

        assertThrows(BadRequestException.class,
                () -> categoryService.deleteCategory(300L, "super@bicap.com"));
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteCategory_empty_shouldDelete() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(categoryRepository.findById(300L)).thenReturn(Optional.of(category));
        when(productRepository.countByCategory()).thenReturn(List.<Object[]>of(new Object[]{300L, 0L}));

        categoryService.deleteCategory(300L, "super@bicap.com");

        verify(categoryRepository).delete(category);
    }

    @Test
    void updateCategory_duplicateName_shouldThrowConflict() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(categoryRepository.findById(300L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Trái cây", 300L)).thenReturn(true);

        CategoryRequest request = new CategoryRequest("Trái cây", null, null);
        assertThrows(ConflictException.class,
                () -> categoryService.updateCategory(300L, request, "super@bicap.com"));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void getAllCategories_shouldIncludeProductCounts() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(productRepository.countByCategory()).thenReturn(List.<Object[]>of(new Object[]{300L, 5L}));

        List<CategoryResponse> result = categoryService.getAllCategories("super@bicap.com");

        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).getProductCount());
        // One aggregate query for all counts — no per-category N+1
        verify(productRepository, times(1)).countByCategory();
    }
}
