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
import vn.courses.ut.edu.javaprogramming.bicap.dto.CancelOrderRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.OrderResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.PlaceOrderRequest;
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
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BICAP-75 — Vòng đời đầy đủ đơn hàng:
 * đặt mua (placeOrder), hủy đơn (cancelOrder),
 * xác nhận giao (confirmDelivery), xác nhận nhận (completeOrder),
 * và Retailer xem danh sách đơn (getRetailerOrders).
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class OrderManagementServiceTest {

    private static final long FM_ID       = 7L;
    private static final long FARM_ID     = 2L;
    private static final long SEASON_ID   = 9L;
    private static final long PRODUCT_ID  = 1L;
    private static final long RETAILER_ID = 10L;
    private static final long ORDER_ID    = 5L;

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private FarmingSeasonRepository seasonRepository;
    @Mock private FarmRepository farmRepository;
    @Mock private NotificationService notificationService;

    private OrderService service;
    private User retailer;
    private User farmManager;
    private Farm farm;
    private FarmingSeason season;
    private Product product;

    @BeforeEach
    void setUp() {
        service = new OrderService(orderRepository, userRepository, new SepayConfig(),
                productRepository, seasonRepository, farmRepository, notificationService);

        Role retailerRole = Role.builder().name("RETAILER").permissions(Set.of()).build();
        retailer = User.builder().id(RETAILER_ID).email("retailer@bicap.com")
                .fullName("Nhà Bán Lẻ A").phone("0900000000")
                .status(UserStatus.ACTIVE).roles(Set.of(retailerRole)).build();

        Role fmRole = Role.builder().name("FARM_MANAGER").permissions(Set.of()).build();
        farmManager = User.builder().id(FM_ID).email("farm@bicap.com")
                .fullName("Farm Manager X").status(UserStatus.ACTIVE).roles(Set.of(fmRole)).build();

        farm = Farm.builder().id(FARM_ID).userId(FM_ID).name("Trang Trại Xanh").address("A").area(1d).build();
        season = new FarmingSeason(SEASON_ID, FARM_ID, "Mùa Cải", "Rau ăn lá", "Cải xanh",
                1.0, LocalDate.now(), null, "HARVESTED", null, null);
        product = Product.builder()
                .id(PRODUCT_ID).seasonId(SEASON_ID).name("Cải xanh hữu cơ")
                .price(new BigDecimal("15000")).quantity(100.0)
                .status("ACTIVE")
                .build();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void loginAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private Order depositPaidOrder() {
        Order o = new Order();
        o.setId(ORDER_ID);
        o.setProductId(PRODUCT_ID);
        o.setRetailerId(RETAILER_ID);
        o.setQuantity(50.0);
        o.setPrice(new BigDecimal("15000"));
        o.setDeliveryAddr("Hà Nội");
        o.setDepositAmount(new BigDecimal("225000.00"));
        o.setStatus(Order.STATUS_DEPOSIT_PAID);
        return o;
    }

    private Order pendingOrder() {
        Order o = new Order();
        o.setId(ORDER_ID);
        o.setProductId(PRODUCT_ID);
        o.setRetailerId(RETAILER_ID);
        o.setQuantity(50.0);
        o.setPrice(new BigDecimal("15000"));
        o.setDeliveryAddr("Hà Nội");
        o.setStatus(Order.STATUS_PENDING);
        return o;
    }

    private Order deliveredOrder() {
        Order o = pendingOrder();
        o.setStatus(Order.STATUS_DELIVERED);
        return o;
    }

    private void stubDeliveryContext(Order order) {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.of(season));
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(farm));
    }

    // ── placeOrder ───────────────────────────────────────────────────────────

    @Test
    void placeOrder_createsOrderWithPendingStatus_andNotifiesFarmManager() {
        loginAs(retailer);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.of(season));
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(farm));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(ORDER_ID);
            return o;
        });

        PlaceOrderRequest req = new PlaceOrderRequest(PRODUCT_ID, 30.0, "TP.HCM");
        OrderResponse response = service.placeOrder(req);

        assertEquals(Order.STATUS_PENDING, response.getStatus());
        assertEquals(new BigDecimal("15000"), response.getPrice());
        assertEquals(30.0, response.getQuantity());
        verify(notificationService).sendNotification(eq(FM_ID), eq("INFO"), anyString(), anyString(), eq(false));
    }

    @Test
    void placeOrder_throwsNotFound_whenProductMissing() {
        loginAs(retailer);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.placeOrder(new PlaceOrderRequest(PRODUCT_ID, 10.0, "HN")));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_throwsBadRequest_whenProductNotActive() {
        loginAs(retailer);
        product = Product.builder()
                .id(PRODUCT_ID).seasonId(SEASON_ID).name("Sản phẩm cũ")
                .price(new BigDecimal("10000")).quantity(100.0)
                .status("INACTIVE")
                .build();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThrows(BadRequestException.class,
                () -> service.placeOrder(new PlaceOrderRequest(PRODUCT_ID, 10.0, "HN")));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_throwsForbidden_whenNotRetailer() {
        loginAs(farmManager); // Farm Manager không được đặt hàng

        assertThrows(ForbiddenException.class,
                () -> service.placeOrder(new PlaceOrderRequest(PRODUCT_ID, 10.0, "HN")));
        verifyNoInteractions(productRepository, orderRepository);
    }

    // ── cancelOrder ──────────────────────────────────────────────────────────

    @Test
    void cancelOrder_allowsRetailerOnPendingOrder() {
        loginAs(retailer);
        Order order = pendingOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.of(season));
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(farm));
        when(userRepository.findById(RETAILER_ID)).thenReturn(Optional.of(retailer));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = service.cancelOrder(ORDER_ID, new CancelOrderRequest("Thay đổi kế hoạch"));

        assertEquals(Order.STATUS_CANCELLED, response.getStatus());
        assertEquals(Order.STATUS_CANCELLED, order.getStatus());
        assertEquals("Thay đổi kế hoạch", order.getCancelledReason());
        verify(notificationService).sendNotification(eq(FM_ID), eq("WARNING"), anyString(), anyString(), eq(false));
    }

    @Test
    void cancelOrder_allowsRetailerOnAcceptedOrder() {
        loginAs(retailer);
        Order order = pendingOrder();
        order.setStatus(Order.STATUS_ACCEPTED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.of(season));
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(farm));
        when(userRepository.findById(RETAILER_ID)).thenReturn(Optional.of(retailer));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = service.cancelOrder(ORDER_ID, new CancelOrderRequest("Không còn nhu cáº§u"));

        assertEquals(Order.STATUS_CANCELLED, response.getStatus());
        assertEquals("Không còn nhu cáº§u", order.getCancelledReason());
    }

    @Test
    void cancelOrder_requiresReason() {
        loginAs(retailer);

        assertThrows(BadRequestException.class,
                () -> service.cancelOrder(ORDER_ID, new CancelOrderRequest("  ")));
        verifyNoInteractions(orderRepository);
    }

    @Test
    void cancelOrder_throwsForbidden_forAnotherRetailersOrder() {
        loginAs(retailer);
        Order order = pendingOrder();
        order.setRetailerId(999L); // đơn của người khác
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(ForbiddenException.class,
                () -> service.cancelOrder(ORDER_ID, new CancelOrderRequest("Äáº·t nháº§m")));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_requestsAdminReview_whenDepositAlreadyPaid() {
        loginAs(retailer);
        Order order = depositPaidOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.of(season));
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(farm));
        Role adminRole = Role.builder().name("ADMIN").permissions(Set.of()).build();
        User admin = User.builder().id(99L).email("admin@bicap.com").roles(Set.of(adminRole)).build();
        when(userRepository.findDistinctByRoles_NameIn(anyCollection())).thenReturn(java.util.List.of(admin));
        when(userRepository.findById(RETAILER_ID)).thenReturn(Optional.of(retailer));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = service.cancelOrder(ORDER_ID, new CancelOrderRequest("Change plan"));

        assertEquals(Order.STATUS_CANCEL_REQUESTED, response.getStatus());
        assertEquals("Change plan", order.getCancelledReason());
        assertNotNull(order.getCancelRequestedAt());
        verify(notificationService).sendNotification(eq(99L), eq("WARNING"), anyString(), anyString(), eq(false));
    }

    // ── confirmDelivery ──────────────────────────────────────────────────────

    @Test
    void markInTransit_transitionsDepositPaidOrder_andNotifiesRetailer() {
        loginAs(farmManager);
        Order order = depositPaidOrder();
        stubDeliveryContext(order);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = service.markInTransit(ORDER_ID);

        assertEquals(Order.STATUS_IN_TRANSIT, response.getStatus());
        verify(notificationService).sendNotification(eq(RETAILER_ID), eq("INFO"), anyString(), anyString(), eq(false));
    }

    @Test
    void confirmDelivery_transitionsToDelivered_andNotifiesRetailer() {
        loginAs(farmManager);
        Order order = depositPaidOrder();
        order.setStatus(Order.STATUS_IN_TRANSIT);
        stubDeliveryContext(order);
        when(userRepository.findById(RETAILER_ID)).thenReturn(Optional.of(retailer));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = service.confirmDelivery(ORDER_ID);

        assertEquals(Order.STATUS_DELIVERED, response.getStatus());
        assertEquals(Order.STATUS_DELIVERED, order.getStatus());
        assertNotNull(order.getDeliveredAt());
        verify(notificationService).sendNotification(eq(RETAILER_ID), eq("INFO"), anyString(), anyString(), eq(false));
    }

    @Test
    void confirmDelivery_throwsBadRequest_whenNotInTransit() {
        loginAs(farmManager);
        Order order = pendingOrder(); // vẫn PENDING
        stubDeliveryContext(order);

        assertThrows(BadRequestException.class,
                () -> service.confirmDelivery(ORDER_ID));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void confirmDelivery_throwsForbidden_forAnotherFarmsOrder() {
        loginAs(farmManager);
        Order order = depositPaidOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.of(season));
        Farm otherFarm = Farm.builder().id(FARM_ID).userId(999L).name("Farm Khác").address("B").area(1d).build();
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(otherFarm));

        assertThrows(ForbiddenException.class, () -> service.confirmDelivery(ORDER_ID));
        verify(orderRepository, never()).save(any());
    }

    // ── completeOrder ────────────────────────────────────────────────────────

    @Test
    void completeOrder_transitionsDeliveredToCompleted_andNotifiesFarmManager() {
        loginAs(retailer);
        Order order = deliveredOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.of(season));
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(farm));
        when(userRepository.findById(RETAILER_ID)).thenReturn(Optional.of(retailer));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = service.completeOrder(ORDER_ID);

        assertEquals(Order.STATUS_COMPLETED, response.getStatus());
        assertEquals(Order.STATUS_COMPLETED, order.getStatus());
        assertNotNull(order.getCompletedAt());
        verify(notificationService).sendNotification(eq(FM_ID), eq("SUCCESS"), anyString(), anyString(), eq(false));
    }

    @Test
    void completeOrder_throwsForbidden_whenNotOwner() {
        loginAs(retailer);
        Order order = deliveredOrder();
        order.setRetailerId(999L); // đơn của người khác
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(ForbiddenException.class, () -> service.completeOrder(ORDER_ID));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void completeOrder_throwsBadRequest_whenNotDelivered() {
        loginAs(retailer);
        Order order = pendingOrder(); // chưa giao
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class, () -> service.completeOrder(ORDER_ID));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void completeOrder_throwsForbidden_whenCalledByFarmManager() {
        loginAs(farmManager);

        assertThrows(ForbiddenException.class, () -> service.completeOrder(ORDER_ID));
        verifyNoInteractions(orderRepository);
    }

    // ── getRetailerOrders ─────────────────────────────────────────────────────

    @Test
    void getRetailerOrders_returnsOnlyCurrentRetailersOrders() {
        loginAs(retailer);
        Order order = pendingOrder();
        when(orderRepository.findRetailerOrders(RETAILER_ID, "PENDING")).thenReturn(java.util.List.of(order));
        when(productRepository.findAllById(java.util.List.of(PRODUCT_ID))).thenReturn(java.util.List.of(product));
        when(seasonRepository.findAllById(java.util.List.of(SEASON_ID))).thenReturn(java.util.List.of(season));
        when(farmRepository.findAllById(java.util.List.of(FARM_ID))).thenReturn(java.util.List.of(farm));

        var result = service.getRetailerOrders("pending");

        assertEquals(1, result.size());
        assertEquals(ORDER_ID, result.get(0).getId());
        assertEquals("Cải xanh hữu cơ", result.get(0).getProductName());
    }

    @Test
    void getRetailerOrders_throwsForbidden_whenCalledByFarmManager() {
        loginAs(farmManager);

        assertThrows(ForbiddenException.class, () -> service.getRetailerOrders(null));
        verifyNoInteractions(orderRepository);
    }

    @Test
    void getRetailerOrderDetail_returnsOwnedOrder() {
        loginAs(retailer);
        Order order = pendingOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.of(season));
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(farm));

        OrderResponse response = service.getRetailerOrderDetail(ORDER_ID);

        assertEquals(ORDER_ID, response.getId());
        assertEquals(product.getName(), response.getProductName());
        assertEquals(farm.getName(), response.getFarmName());
    }

    @Test
    void getRetailerOrderDetail_rejectsAnotherRetailersOrder() {
        loginAs(retailer);
        Order order = pendingOrder();
        order.setRetailerId(999L);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(ForbiddenException.class,
                () -> service.getRetailerOrderDetail(ORDER_ID));
        verifyNoInteractions(productRepository, seasonRepository, farmRepository);
    }
}
