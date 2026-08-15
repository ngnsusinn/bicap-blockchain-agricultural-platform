package vn.courses.ut.edu.javaprogramming.bicap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.courses.ut.edu.javaprogramming.bicap.dto.RetailerPartnerDetailResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.RetailerPartnerResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.BusinessType;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Product;
import vn.courses.ut.edu.javaprogramming.bicap.entity.RetailerBusinessProfile;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Role;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingSeasonRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.OrderRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ProductRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.RetailerBusinessProfileRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.RetailerPartnerService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BICAP-21 / SRS-FM-015 — Farm Manager xem thông tin Nhà bán lẻ đã ký hợp đồng:
 * danh sách đối tác (gộp theo Retailer, chỉ số giao dịch, chỉ tính farm của user),
 * chi tiết kèm lịch sử giao dịch, và kiểm soát quyền FARM_MANAGER.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class RetailerPartnerServiceTest {

    private static final long FM_ID = 7L;
    private static final long FARM_ID = 2L;
    private static final long SEASON_ID = 9L;
    private static final long PRODUCT_ID = 1L;
    private static final long RETAILER_ID = 10L;
    private static final long RETAILER_2_ID = 11L;
    private static final long ORDER_ID = 5L;

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private RetailerBusinessProfileRepository businessProfileRepository;
    @Mock private ProductRepository productRepository;
    @Mock private FarmingSeasonRepository seasonRepository;
    @Mock private FarmRepository farmRepository;

    private RetailerPartnerService service;
    private User farmManager;
    private User retailer;
    private RetailerBusinessProfile businessProfile;
    private Farm farm;
    private FarmingSeason season;
    private Product product;

    @BeforeEach
    void setUp() {
        service = new RetailerPartnerService(orderRepository, userRepository, businessProfileRepository,
                productRepository, seasonRepository, farmRepository);

        Role role = Role.builder().name("FARM_MANAGER").permissions(Set.of()).build();
        farmManager = User.builder().id(FM_ID).email("farm@bicap.com").status(UserStatus.ACTIVE).roles(Set.of(role)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(farmManager, null, farmManager.getAuthorities()));

        Role retailerRole = Role.builder().name("RETAILER").permissions(Set.of()).build();
        retailer = User.builder().id(RETAILER_ID).email("retailer@bicap.com").fullName("Nhà Bán Lẻ A")
                .phone("0900000000").status(UserStatus.ACTIVE).roles(Set.of(retailerRole)).build();

        businessProfile = new RetailerBusinessProfile();
        businessProfile.setId(1L);
        businessProfile.setUser(retailer);
        businessProfile.setBusinessName("Siêu Thị Xanh A");
        businessProfile.setAddress("Quận 1, TP.HCM");
        businessProfile.setBusinessType(BusinessType.SUPERMARKET);
        businessProfile.setLicenseUrl("https://bicap.vn/docs/license-a.pdf");

        farm = Farm.builder().id(FARM_ID).userId(FM_ID).name("Trang Trại Xanh").address("A").area(1d).build();
        season = new FarmingSeason(SEASON_ID, FARM_ID, "Mùa Cải", "Rau ăn lá", "Cải xanh",
                1.0, LocalDate.now(), null, "HARVESTED", null, null);
        product = Product.builder().id(PRODUCT_ID).seasonId(SEASON_ID).name("Cải xanh hữu cơ")
                .price(new BigDecimal("15000")).quantity(100.0).build();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private Order order(long orderId, long retailerId, String status, LocalDateTime createdAt) {
        Order order = new Order();
        order.setId(orderId);
        order.setProductId(PRODUCT_ID);
        order.setRetailerId(retailerId);
        order.setQuantity(50.0);
        order.setPrice(new BigDecimal("15000"));
        order.setStatus(status);
        order.setCreatedAt(createdAt);
        return order;
    }

    // ── Danh sách ──

    @Test
    void getContractRetailers_groupsOrdersByRetailer_andComputesStats() {
        LocalDateTime t1 = LocalDateTime.of(2026, 7, 1, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 8, 1, 10, 0);
        when(orderRepository.findFarmManagerOrders(FM_ID, null))
                .thenReturn(List.of(order(1L, RETAILER_ID, Order.STATUS_PENDING, t1),
                        order(2L, RETAILER_ID, Order.STATUS_ACCEPTED, t2)));
        when(userRepository.findAllById(Set.of(RETAILER_ID))).thenReturn(List.of(retailer));
        when(businessProfileRepository.findByUserIdIn(Set.of(RETAILER_ID))).thenReturn(List.of(businessProfile));

        List<RetailerPartnerResponse> result = service.getContractRetailers();

        assertEquals(1, result.size());
        RetailerPartnerResponse r = result.get(0);
        assertEquals(RETAILER_ID, r.getRetailerId());
        assertEquals("Nhà Bán Lẻ A", r.getRetailerName());
        assertEquals("Siêu Thị Xanh A", r.getBusinessName());
        assertEquals(BusinessType.SUPERMARKET, r.getBusinessType());
        assertEquals("https://bicap.vn/docs/license-a.pdf", r.getLicenseUrl());
        assertEquals(2, r.getTotalOrders());
        assertEquals(new BigDecimal("1500000.0"), r.getTotalSpent()); // 2 × 50 × 15.000
        assertEquals(t1, r.getFirstOrderAt());
        assertEquals(t2, r.getLastOrderAt());
    }

    @Test
    void getContractRetailers_handlesMultipleRetailers_sortedByLatestTransaction() {
        LocalDateTime t1 = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 8, 1, 10, 0);
        Role retailerRole = Role.builder().name("RETAILER").permissions(Set.of()).build();
        User retailerB = User.builder().id(RETAILER_2_ID).email("retailer2@bicap.com").fullName("Nhà Bán Lẻ B")
                .status(UserStatus.ACTIVE).roles(Set.of(retailerRole)).build();
        when(orderRepository.findFarmManagerOrders(FM_ID, null))
                .thenReturn(List.of(order(1L, RETAILER_ID, Order.STATUS_PENDING, t2),
                        order(2L, RETAILER_2_ID, Order.STATUS_PENDING, t1)));
        when(userRepository.findAllById(Set.of(RETAILER_ID, RETAILER_2_ID)))
                .thenReturn(List.of(retailer, retailerB));
        // Chỉ Retailer A có hồ sơ kinh doanh — Retailer B phải null-safe.
        when(businessProfileRepository.findByUserIdIn(Set.of(RETAILER_ID, RETAILER_2_ID)))
                .thenReturn(List.of(businessProfile));

        List<RetailerPartnerResponse> result = service.getContractRetailers();

        assertEquals(2, result.size());
        assertEquals(RETAILER_ID, result.get(0).getRetailerId());   // giao dịch gần nhất trước
        assertEquals(RETAILER_2_ID, result.get(1).getRetailerId());
        assertNull(result.get(1).getBusinessName());
        assertEquals(1, result.get(1).getTotalOrders());
    }

    @Test
    void getContractRetailers_returnsEmpty_whenNoOrders() {
        when(orderRepository.findFarmManagerOrders(FM_ID, null)).thenReturn(List.of());

        assertTrue(service.getContractRetailers().isEmpty());
        verifyNoInteractions(userRepository);
    }

    @Test
    void getContractRetailers_requiresFarmManagerRole() {
        Role retailerRole = Role.builder().name("RETAILER").permissions(Set.of()).build();
        User retailer = User.builder().id(99L).email("retailer@bicap.com")
                .status(UserStatus.ACTIVE).roles(Set.of(retailerRole)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(retailer, null, retailer.getAuthorities()));

        assertThrows(ForbiddenException.class, () -> service.getContractRetailers());
        verifyNoInteractions(orderRepository);
    }

    // ── Chi tiết ──

    @Test
    void getContractRetailerDetail_returnsSummaryAndTransactions() {
        LocalDateTime t = LocalDateTime.of(2026, 8, 1, 10, 0);
        when(orderRepository.findFarmManagerRetailerOrders(FM_ID, RETAILER_ID))
                .thenReturn(List.of(order(ORDER_ID, RETAILER_ID, Order.STATUS_DEPOSIT_PAID, t)));
        when(userRepository.findById(RETAILER_ID)).thenReturn(Optional.of(retailer));
        when(businessProfileRepository.findByUserId(RETAILER_ID)).thenReturn(Optional.of(businessProfile));
        when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(product));
        when(seasonRepository.findAllById(List.of(SEASON_ID))).thenReturn(List.of(season));
        when(farmRepository.findAllById(List.of(FARM_ID))).thenReturn(List.of(farm));

        RetailerPartnerDetailResponse detail = service.getContractRetailerDetail(RETAILER_ID);

        assertEquals(RETAILER_ID, detail.getRetailer().getRetailerId());
        assertEquals("Siêu Thị Xanh A", detail.getRetailer().getBusinessName());
        assertEquals(1, detail.getTransactions().size());
        assertEquals(ORDER_ID, detail.getTransactions().get(0).getOrderId());
        assertEquals("Cải xanh hữu cơ", detail.getTransactions().get(0).getProductName());
        assertEquals("Trang Trại Xanh", detail.getTransactions().get(0).getFarmName());
        assertEquals(new BigDecimal("750000.0"), detail.getTransactions().get(0).getTotalAmount());
    }

    @Test
    void getContractRetailerDetail_throwsNotFound_whenRetailerHasNoContractWithMyFarms() {
        when(orderRepository.findFarmManagerRetailerOrders(FM_ID, RETAILER_ID)).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> service.getContractRetailerDetail(RETAILER_ID));
        verifyNoInteractions(userRepository);
    }

    @Test
    void getContractRetailerDetail_throwsForbidden_forNonFarmManager() {
        Role retailerRole = Role.builder().name("RETAILER").permissions(Set.of()).build();
        User retailer = User.builder().id(99L).email("retailer@bicap.com")
                .status(UserStatus.ACTIVE).roles(Set.of(retailerRole)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(retailer, null, retailer.getAuthorities()));

        assertThrows(ForbiddenException.class, () -> service.getContractRetailerDetail(RETAILER_ID));
        verifyNoInteractions(orderRepository);
    }
}
