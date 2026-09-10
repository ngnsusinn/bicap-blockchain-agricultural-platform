package vn.courses.ut.edu.javaprogramming.bicap;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentDetailResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.*;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.*;
import vn.courses.ut.edu.javaprogramming.bicap.service.RetailerShipmentService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho RetailerShipmentService (BICAP-49 / SRS-RT-014).
 *
 * Kiểm tra các tình huống:
 *   1. Retailer lấy đúng danh sách shipment của chính mình
 *   2. Retailer bị chặn khi cố lấy shipment của retailer khác
 *   3. Role không phải RETAILER bị từ chối (ForbiddenException)
 *   4. Shipment không tồn tại → ResourceNotFoundException
 *   5. Shipment detail trả đúng khi ownership hợp lệ
 *   6. Filter theo status hoạt động đúng
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class RetailerShipmentServiceTest {

    @Mock ShipmentRepository shipments;
    @Mock ShipmentTrackingRepository tracking;
    @Mock OrderRepository orders;
    @Mock DriverRepository drivers;
    @Mock VehicleRepository vehicles;
    @Mock UserRepository users;

    RetailerShipmentService service;

    @BeforeEach
    void setUp() {
        service = new RetailerShipmentService(shipments, tracking, orders, drivers, vehicles, users);
    }

    @AfterEach
    void clearCtx() { SecurityContextHolder.clearContext(); }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Đăng nhập giả lập với vai trò RETAILER, id = retailerId. */
    private void loginRetailer(Long retailerId) {
        Role role = Role.builder().name("RETAILER").permissions(Set.of()).build();
        User u = User.builder()
                .id(retailerId)
                .email("retailer" + retailerId + "@bicap.vn")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(role))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
    }

    /** Đăng nhập giả lập với vai trò FARM_MANAGER. */
    private void loginFarmManager(Long userId) {
        Role role = Role.builder().name("FARM_MANAGER").permissions(Set.of()).build();
        User u = User.builder()
                .id(userId)
                .email("farm@bicap.vn")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(role))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
    }

    /** Tạo Shipment giả đơn giản. */
    private Shipment makeShipment(Long id, Long orderId, String status) {
        Shipment s = new Shipment();
        s.setId(id);
        s.setOrderId(orderId);
        s.setStatus(status);
        s.setCreatedAt(LocalDateTime.now());
        return s;
    }

    /** Tạo Order gán cho retailerId. */
    private vn.courses.ut.edu.javaprogramming.bicap.entity.Order makeOrder(Long orderId, Long retailerId) {
        vn.courses.ut.edu.javaprogramming.bicap.entity.Order o = new vn.courses.ut.edu.javaprogramming.bicap.entity.Order();
        o.setId(orderId);
        o.setRetailerId(retailerId);
        o.setDeliveryAddr("123 Nguyễn Huệ, TP.HCM");
        return o;
    }

    // ── Test cases ────────────────────────────────────────────────────────────

    /**
     * TC-01: Retailer A (id=10) gọi getMyShipments → chỉ nhận được shipment
     * của đơn hàng thuộc mình, đúng thông tin.
     */
    @Test
    @DisplayName("TC-01: getMyShipments trả đúng danh sách của retailer hiện tại")
    void getMyShipments_returnsOwnShipments() {
        loginRetailer(10L);

        Shipment s1 = makeShipment(1L, 100L, Shipment.STATUS_IN_TRANSIT);
        Shipment s2 = makeShipment(2L, 101L, Shipment.STATUS_DELIVERED);
        when(shipments.findByRetailerId(eq(10L), isNull()))
                .thenReturn(List.of(s1, s2));

        vn.courses.ut.edu.javaprogramming.bicap.entity.Order o1 = makeOrder(100L, 10L);
        vn.courses.ut.edu.javaprogramming.bicap.entity.Order o2 = makeOrder(101L, 10L);
        when(orders.findById(100L)).thenReturn(Optional.of(o1));
        when(orders.findById(101L)).thenReturn(Optional.of(o2));

        List<ShipmentResponse> result = service.getMyShipments(null);

        assertEquals(2, result.size(), "Phải trả về đúng 2 shipment");
        assertEquals(1L,  result.get(0).getId());
        assertEquals(2L,  result.get(1).getId());
        assertEquals("123 Nguyễn Huệ, TP.HCM", result.get(0).getDeliveryAddr());
        verify(shipments).findByRetailerId(10L, null);
    }

    /**
     * TC-02: Retailer A gọi getMyShipments với filter "IN_TRANSIT" →
     * query được truyền đúng status uppercase.
     */
    @Test
    @DisplayName("TC-02: getMyShipments lọc đúng theo status")
    void getMyShipments_filtersStatus() {
        loginRetailer(10L);
        when(shipments.findByRetailerId(eq(10L), eq("IN_TRANSIT"))).thenReturn(List.of());

        service.getMyShipments("in_transit"); // lowercase → phải normalize thành IN_TRANSIT

        verify(shipments).findByRetailerId(10L, "IN_TRANSIT");
    }

    /**
     * TC-03: Role FARM_MANAGER gọi getMyShipments → ForbiddenException
     * vì endpoint chỉ dành cho RETAILER.
     */
    @Test
    @DisplayName("TC-03: FARM_MANAGER không được phép gọi getMyShipments")
    void getMyShipments_rejectsFarmManagerRole() {
        loginFarmManager(7L);

        assertThrows(ForbiddenException.class, () -> service.getMyShipments(null));
        verifyNoInteractions(shipments);
    }

    /**
     * TC-04: getMyShipmentDetail với shipmentId không tồn tại →
     * ResourceNotFoundException.
     */
    @Test
    @DisplayName("TC-04: getMyShipmentDetail ném ResourceNotFoundException nếu shipment không tồn tại")
    void getMyShipmentDetail_shipmentNotFound() {
        loginRetailer(10L);
        when(shipments.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getMyShipmentDetail(999L));
    }

    /**
     * TC-05: Retailer A (id=10) gọi getMyShipmentDetail cho shipment của
     * Retailer B (id=20) → ForbiddenException (IDOR protection).
     */
    @Test
    @DisplayName("TC-05: getMyShipmentDetail chặn truy cập shipment của retailer khác (IDOR)")
    void getMyShipmentDetail_rejectsOtherRetailersShipment() {
        loginRetailer(10L);

        Shipment s = makeShipment(5L, 200L, Shipment.STATUS_IN_TRANSIT);
        when(shipments.findById(5L)).thenReturn(Optional.of(s));

        // Order 200 thuộc Retailer B (id=20), không phải Retailer A (id=10)
        vn.courses.ut.edu.javaprogramming.bicap.entity.Order orderOfOtherRetailer = makeOrder(200L, 20L);
        when(orders.findById(200L)).thenReturn(Optional.of(orderOfOtherRetailer));

        assertThrows(ForbiddenException.class,
                () -> service.getMyShipmentDetail(5L));
    }

    /**
     * TC-06: Retailer A (id=10) gọi getMyShipmentDetail cho shipment hợp lệ →
     * trả về detail đúng với tracking history.
     */
    @Test
    @DisplayName("TC-06: getMyShipmentDetail trả đúng chi tiết và tracking khi ownership hợp lệ")
    void getMyShipmentDetail_returnsDetailForOwnShipment() {
        loginRetailer(10L);

        Shipment s = makeShipment(5L, 200L, Shipment.STATUS_DELIVERED);
        when(shipments.findById(5L)).thenReturn(Optional.of(s));

        vn.courses.ut.edu.javaprogramming.bicap.entity.Order order = makeOrder(200L, 10L); // thuộc Retailer A
        when(orders.findById(200L)).thenReturn(Optional.of(order));

        when(tracking.findByShipmentIdOrderByTimestampDesc(5L)).thenReturn(List.of());

        ShipmentDetailResponse detail = service.getMyShipmentDetail(5L);

        assertNotNull(detail);
        assertEquals(5L, detail.getId());
        assertEquals(Shipment.STATUS_DELIVERED, detail.getStatus());
        assertEquals("123 Nguyễn Huệ, TP.HCM", detail.getDeliveryAddr());
    }

    /**
     * TC-07: Shipment không có orderId → ForbiddenException
     * (dữ liệu bất nhất, không thể verify ownership).
     */
    @Test
    @DisplayName("TC-07: Shipment không có orderId → ForbiddenException")
    void getMyShipmentDetail_noOrderId_throws() {
        loginRetailer(10L);

        Shipment s = new Shipment();
        s.setId(6L);
        s.setOrderId(null); // không có orderId
        s.setStatus(Shipment.STATUS_IN_TRANSIT);
        when(shipments.findById(6L)).thenReturn(Optional.of(s));

        assertThrows(ForbiddenException.class,
                () -> service.getMyShipmentDetail(6L));
    }
}
