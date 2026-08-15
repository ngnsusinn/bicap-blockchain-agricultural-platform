package vn.courses.ut.edu.javaprogramming.bicap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.courses.ut.edu.javaprogramming.bicap.config.SepayConfig;
import vn.courses.ut.edu.javaprogramming.bicap.dto.OrderResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Product;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Role;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingSeasonRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.OrderRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ProductRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.NotificationService;
import vn.courses.ut.edu.javaprogramming.bicap.service.OrderService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BICAP-20 / SRS-FM-014 — Farm Manager xử lý yêu cầu mua nông sản từ Retailer:
 * danh sách theo farm, chi tiết (ownership), chấp nhận (BR2 tồn kho, thông báo Retailer),
 * từ chối (bắt buộc lý do, thông báo Retailer).
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class FarmManagerOrderServiceTest {

    private static final long FM_ID = 7L;
    private static final long FARM_ID = 2L;
    private static final long SEASON_ID = 9L;
    private static final long PRODUCT_ID = 1L;
    private static final long RETAILER_ID = 10L;
    private static final long ORDER_ID = 5L;

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private FarmingSeasonRepository seasonRepository;
    @Mock private FarmRepository farmRepository;
    @Mock private NotificationService notificationService;

    private OrderService service;
    private User farmManager;
    private User retailer;
    private Farm farm;
    private FarmingSeason season;
    private Product product;

    @BeforeEach
    void setUp() {
        service = new OrderService(orderRepository, userRepository, new SepayConfig(),
                productRepository, seasonRepository, farmRepository, notificationService);

        Role role = Role.builder().name("FARM_MANAGER").permissions(Set.of()).build();
        farmManager = User.builder().id(FM_ID).email("farm@bicap.com").status(UserStatus.ACTIVE).roles(Set.of(role)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(farmManager, null, farmManager.getAuthorities()));

        Role retailerRole = Role.builder().name("RETAILER").permissions(Set.of()).build();
        retailer = User.builder().id(RETAILER_ID).email("retailer@bicap.com").fullName("Nhà Bán Lẻ A")
                .phone("0900000000").status(UserStatus.ACTIVE).roles(Set.of(retailerRole)).build();

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

    private Order pendingOrder() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setProductId(PRODUCT_ID);
        order.setRetailerId(RETAILER_ID);
        order.setQuantity(50.0);
        order.setPrice(new BigDecimal("15000"));
        order.setDeliveryAddr("Hà Nội");
        order.setStatus(Order.STATUS_PENDING);
        return order;
    }

    /** Stubs the order→product→season chain; the farm is stubbed separately per test (ownership varies). */
    private void stubOrderContext(Order order) {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.of(season));
    }

    // ── Danh sách ──

    @Test
    void getFarmManagerOrders_filtersByStatusAndScopesToOwnFarms() {
        Order pending = pendingOrder();
        when(orderRepository.findFarmManagerOrders(FM_ID, "PENDING")).thenReturn(List.of(pending));
        when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(product));
        when(seasonRepository.findAllById(List.of(SEASON_ID))).thenReturn(List.of(season));
        when(farmRepository.findAllById(List.of(FARM_ID))).thenReturn(List.of(farm));
        when(userRepository.findAllById(List.of(RETAILER_ID))).thenReturn(List.of(retailer));

        List<OrderResponse> result = service.getFarmManagerOrders("pending");

        assertEquals(1, result.size());
        assertEquals(ORDER_ID, result.get(0).getId());
        assertEquals("Cải xanh hữu cơ", result.get(0).getProductName());
        assertEquals("Trang Trại Xanh", result.get(0).getFarmName());
        assertEquals(RETAILER_ID, result.get(0).getRetailerId());
        assertEquals("Nhà Bán Lẻ A", result.get(0).getRetailerName());
        assertEquals(new BigDecimal("750000.0"), result.get(0).getTotalAmount());
    }

    @Test
    void getFarmManagerOrders_withoutStatusReturnsAll() {
        when(orderRepository.findFarmManagerOrders(FM_ID, null)).thenReturn(List.of());

        assertTrue(service.getFarmManagerOrders(null).isEmpty());
    }

    @Test
    void getFarmManagerOrders_requiresFarmManagerRole() {
        Role retailerRole = Role.builder().name("RETAILER").permissions(Set.of()).build();
        User retailer = User.builder().id(99L).email("retailer@bicap.com")
                .status(UserStatus.ACTIVE).roles(Set.of(retailerRole)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(retailer, null, retailer.getAuthorities()));

        assertThrows(ForbiddenException.class, () -> service.getFarmManagerOrders(null));
        verifyNoInteractions(orderRepository);
    }

    // ── Chi tiết ──

    @Test
    void getOrderDetail_returnsDetailForOwnedOrder() {
        stubOrderContext(pendingOrder());
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(farm));

        OrderResponse response = service.getOrderDetail(ORDER_ID);

        assertEquals(ORDER_ID, response.getId());
        assertEquals("Cải xanh hữu cơ", response.getProductName());
    }

    @Test
    void getOrderDetail_throwsForbidden_forOrderOfAnotherFarm() {
        Farm otherFarm = Farm.builder().id(FARM_ID).userId(999L).name("Farm Khác").address("B").area(1d).build();
        stubOrderContext(pendingOrder());
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(otherFarm));

        assertThrows(ForbiddenException.class, () -> service.getOrderDetail(ORDER_ID));
    }

    @Test
    void getOrderDetail_throwsNotFound_whenOrderMissing() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getOrderDetail(ORDER_ID));
    }

    // ── Chấp nhận ──

    @Test
    void acceptOrder_transitionsPendingToAccepted_andNotifiesRetailer() {
        Order order = pendingOrder();
        stubOrderContext(order);
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(farm));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = service.acceptOrder(ORDER_ID);

        assertEquals(Order.STATUS_ACCEPTED, response.getStatus());
        assertEquals(Order.STATUS_ACCEPTED, order.getStatus());
        verify(notificationService).sendNotification(eq(RETAILER_ID), eq("SUCCESS"), anyString(), anyString(), eq(false));
    }

    @Test
    void acceptOrder_rejectsQuantityAboveAvailableStock() {
        Order order = pendingOrder();
        order.setQuantity(150.0); // vượt tồn kho 100
        stubOrderContext(order);
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(farm));

        assertThrows(BadRequestException.class, () -> service.acceptOrder(ORDER_ID));
        assertEquals(Order.STATUS_PENDING, order.getStatus());
        verify(orderRepository, never()).save(any());
        verify(notificationService, never()).sendNotification(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void acceptOrder_throwsForbidden_forOrderOfAnotherFarm() {
        stubOrderContext(pendingOrder());
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(
                Farm.builder().id(FARM_ID).userId(999L).name("Farm Khác").address("B").area(1d).build()));

        assertThrows(ForbiddenException.class, () -> service.acceptOrder(ORDER_ID));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void acceptOrder_throwsBadRequest_whenNotPending() {
        Order order = pendingOrder();
        order.setStatus(Order.STATUS_ACCEPTED);
        stubOrderContext(order);
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(farm));

        assertThrows(BadRequestException.class, () -> service.acceptOrder(ORDER_ID));
        verify(orderRepository, never()).save(any());
    }

    // ── Từ chối ──

    @Test
    void rejectOrder_requiresReason() {
        // Reason check runs before the order is loaded, so no repository stubs are needed.
        assertThrows(BadRequestException.class, () -> service.rejectOrder(ORDER_ID, "   "));
        verifyNoInteractions(orderRepository);
        verify(notificationService, never()).sendNotification(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void rejectOrder_transitionsPendingToRejected_storesReason_andNotifiesRetailer() {
        Order order = pendingOrder();
        stubOrderContext(order);
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(farm));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = service.rejectOrder(ORDER_ID, "  Hàng đã bán hết trong mùa vụ  ");

        assertEquals(Order.STATUS_REJECTED, response.getStatus());
        assertEquals("Hàng đã bán hết trong mùa vụ", response.getRejectReason());
        assertEquals("Hàng đã bán hết trong mùa vụ", order.getRejectReason());
        verify(notificationService).sendNotification(eq(RETAILER_ID), eq("WARNING"), anyString(), contains("Hàng đã bán hết trong mùa vụ"), eq(false));
    }

    @Test
    void rejectOrder_throwsBadRequest_whenNotPending() {
        Order order = pendingOrder();
        order.setStatus(Order.STATUS_REJECTED);
        stubOrderContext(order);
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(farm));

        assertThrows(BadRequestException.class, () -> service.rejectOrder(ORDER_ID, "Hết hàng"));
        verify(orderRepository, never()).save(any());
    }
}
