package vn.courses.ut.edu.javaprogramming.bicap;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import vn.courses.ut.edu.javaprogramming.bicap.dto.MarketplaceProductRegisterRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ProductListingResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.*;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.*;
import vn.courses.ut.edu.javaprogramming.bicap.service.LocalFileStorageService;
import vn.courses.ut.edu.javaprogramming.bicap.service.TradingFloorService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BICAP-18 / SRS-FM-012 — unit tests cho TradingFloorService.registerProduct:
 * quyền Farm Manager, lô hàng READY, số lượng &lt;= lô xuất, 1–10 ảnh, tạo sản phẩm PENDING_REVIEW.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class TradingFloorServiceTest {

    @Mock ProductRepository products;
    @Mock SeasonExportRepository exports;
    @Mock FarmRepository farms;
    @Mock CategoryRepository categories;
    @Mock FarmingSeasonRepository seasons;
    @Mock LocalFileStorageService fileStorage;
    TradingFloorService service;

    @BeforeEach
    void setUp() {
        service = new TradingFloorService(products, exports, farms, categories, seasons, fileStorage);
        Role role = Role.builder().name("FARM_MANAGER").permissions(Set.of()).build();
        User user = User.builder().id(7L).email("farm@bicap.vn").status(UserStatus.ACTIVE).roles(Set.of(role)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        // lenient: test registerProduct_requiresFarmManagerRole fails before farm lookup
        lenient().when(farms.findById(2L)).thenReturn(Optional.of(Farm.builder()
                .id(2L).userId(7L).name("Farm").address("A").area(1d).build()));
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private SeasonExport readyExport(Long id, Long farmId, Long seasonId, String qty) {
        SeasonExport e = new SeasonExport();
        ReflectionTestUtils.setField(e, "id", id);
        e.setFarmId(farmId);
        e.setSeasonId(seasonId);
        e.setQuantity(new BigDecimal(qty));
        e.setUnit("kg");
        e.setStatus(ExportStatus.READY);
        e.setTraceHash("0xabc");
        return e;
    }

    private MarketplaceProductRegisterRequest request(Long exportId, String qty) {
        return new MarketplaceProductRegisterRequest(
                exportId, "Cải xanh hữu cơ",
                "Rau cải xanh trồng hữu cơ tại Đồng Nai, thu hoạch mới, an toàn cho gia đình và nhà hàng.",
                new BigDecimal(qty), new BigDecimal("15000"), 3L);
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("images", "a.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    @Test
    void registerProduct_createsPendingReviewListingWithQrFromExport() {
        when(exports.findById(10L)).thenReturn(Optional.of(readyExport(10L, 2L, 9L, "100")));
        when(categories.findById(3L)).thenReturn(Optional.of(Category.builder().id(3L).name("Rau ăn lá").build()));
        when(seasons.findById(9L)).thenReturn(Optional.of(new FarmingSeason(9L, 2L, "Mùa Cải", "Rau ăn lá", "Cải xanh",
                1.0, LocalDate.now(), null, "HARVESTED", null, null)));
        when(products.save(any())).thenAnswer(inv -> { Product p = inv.getArgument(0); p.setId(50L); return p; });
        when(fileStorage.storeProductImage(eq(7L), any())).thenReturn("/uploads/farms/7/products/a.jpg");

        ProductListingResponse result = service.registerProduct(2L, request(10L, "50"), List.of(image()));

        assertEquals("PENDING_REVIEW", result.getStatus());
        assertEquals("0xabc", result.getTraceHash());
        assertEquals(50L, result.getId());
        assertEquals("/uploads/farms/7/products/a.jpg", result.getImages().get(0));
        assertEquals("Rau ăn lá", result.getCategoryName());
        verify(products).save(argThat(p ->
                p.getExportId().equals(10L) && p.getSeasonId().equals(9L)
                        && p.getQuantity().equals(50.0) && p.getPrice().compareTo(new BigDecimal("15000")) == 0));
    }

    @Test
    void registerProduct_rejectsQuantityAboveExportedBatch() {
        when(exports.findById(10L)).thenReturn(Optional.of(readyExport(10L, 2L, 9L, "40")));

        assertThrows(BadRequestException.class, () -> service.registerProduct(2L, request(10L, "41"), List.of(image())));
        verifyNoInteractions(products);
    }

    @Test
    void registerProduct_rejectsExportWithoutReadyQr() {
        SeasonExport pending = readyExport(10L, 2L, 9L, "100");
        pending.setStatus(ExportStatus.BLOCKCHAIN_PENDING);
        when(exports.findById(10L)).thenReturn(Optional.of(pending));

        assertThrows(BadRequestException.class, () -> service.registerProduct(2L, request(10L, "50"), List.of(image())));
        verifyNoInteractions(products);
    }

    @Test
    void registerProduct_rejectsExportOfAnotherFarm() {
        when(exports.findById(10L)).thenReturn(Optional.of(readyExport(10L, 99L, 9L, "100")));

        assertThrows(ForbiddenException.class, () -> service.registerProduct(2L, request(10L, "50"), List.of(image())));
        verifyNoInteractions(products);
    }

    @Test
    void registerProduct_rejectsUnknownExport() {
        when(exports.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.registerProduct(2L, request(10L, "50"), List.of(image())));
        verifyNoInteractions(products);
    }

    @Test
    void registerProduct_requiresAtLeastOneImage() {
        // lenient: image check runs before the export lookup, so this stub is never consumed
        lenient().when(exports.findById(10L)).thenReturn(Optional.of(readyExport(10L, 2L, 9L, "100")));

        assertThrows(BadRequestException.class, () -> service.registerProduct(2L, request(10L, "50"), List.of()));
        verifyNoInteractions(products);
    }

    @Test
    void registerProduct_rejectsMoreThanTenImages() {
        // lenient: image check runs before the export lookup, so this stub is never consumed
        lenient().when(exports.findById(10L)).thenReturn(Optional.of(readyExport(10L, 2L, 9L, "100")));
        List<MultipartFile> many = new ArrayList<>();
        for (int i = 0; i < 11; i++) many.add(image());

        assertThrows(BadRequestException.class, () -> service.registerProduct(2L, request(10L, "50"), many));
        verifyNoInteractions(products);
    }

    @Test
    void registerProduct_requiresFarmManagerRole() {
        Role retailerRole = Role.builder().name("RETAILER").permissions(Set.of()).build();
        User retailer = User.builder().id(8L).email("retailer@bicap.com")
                .status(UserStatus.ACTIVE).roles(Set.of(retailerRole)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(retailer, null, retailer.getAuthorities()));

        assertThrows(ForbiddenException.class, () -> service.registerProduct(2L, request(10L, "50"), List.of(image())));
        verifyNoInteractions(exports, products);
    }
}
