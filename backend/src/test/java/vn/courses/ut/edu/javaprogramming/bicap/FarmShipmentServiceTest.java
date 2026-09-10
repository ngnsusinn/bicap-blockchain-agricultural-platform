package vn.courses.ut.edu.javaprogramming.bicap;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmShipmentSummaryResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Driver;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Role;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Shipment;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Vehicle;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.*;
import vn.courses.ut.edu.javaprogramming.bicap.service.FarmShipmentService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BICAP-22/23 / SRS-FM-016/017 — Farm Manager chỉ xem được lô vận chuyển của nông trại
 * mình sở hữu; báo cáo tổng hợp đếm đúng theo trạng thái và tỷ lệ giao đúng hạn.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class FarmShipmentServiceTest {

    @Mock FarmRepository farms;
    @Mock ShipmentRepository shipments;
    @Mock ShipmentTrackingRepository tracking;
    @Mock OrderRepository orders;
    @Mock DriverRepository drivers;
    @Mock VehicleRepository vehicles;
    @Mock UserRepository users;
    FarmShipmentService service;

    @BeforeEach
    void setUp() {
        service = new FarmShipmentService(farms, shipments, tracking, orders, drivers, vehicles, users);
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private void loginFarmManager(Long userId) {
        Role role = Role.builder().name("FARM_MANAGER").permissions(Set.of()).build();
        User u = User.builder().id(userId).email("farm@bicap.vn").status(UserStatus.ACTIVE).roles(Set.of(role)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
    }

    private Shipment shipment(Long id, Long orderId, String status) {
        Shipment s = new Shipment();
        s.setId(id);
        s.setOrderId(orderId);
        s.setStatus(status);
        s.setCreatedAt(LocalDateTime.now());
        return s;
    }

    @Test
    void getFarmShipments_returnsMappedResponses() {
        loginFarmManager(7L);
        when(farms.findById(2L)).thenReturn(Optional.of(Farm.builder().id(2L).userId(7L).name("Farm").address("A").area(1d).build()));
        when(shipments.findByFarmId(eq(2L), isNull())).thenReturn(List.of(shipment(10L, 5L, Shipment.STATUS_IN_TRANSIT)));
        Order order = new Order();
        order.setId(5L);
        order.setDeliveryAddr("Hà Nội");
        when(orders.findById(5L)).thenReturn(Optional.of(order));

        List<ShipmentResponse> result = service.getFarmShipments(2L, null);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals("Hà Nội", result.get(0).getDeliveryAddr());
    }

    @Test
    void getFarmShipments_rejectsFarmOfAnotherOwner() {
        loginFarmManager(7L);
        when(farms.findById(2L)).thenReturn(Optional.of(Farm.builder().id(2L).userId(999L).name("Other").address("A").area(1d).build()));
        assertThrows(ForbiddenException.class, () -> service.getFarmShipments(2L, null));
    }

    @Test
    void getFarmShipments_requiresFarmManagerRole() {
        Role retailerRole = Role.builder().name("RETAILER").permissions(Set.of()).build();
        User retailer = User.builder().id(8L).email("r@bicap.vn").status(UserStatus.ACTIVE).roles(Set.of(retailerRole)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(retailer, null, retailer.getAuthorities()));
        assertThrows(ForbiddenException.class, () -> service.getFarmShipments(2L, null));
        verifyNoInteractions(shipments);
    }

    @Test
    void getFarmShipmentDetail_notFound() {
        loginFarmManager(7L);
        when(farms.findById(2L)).thenReturn(Optional.of(Farm.builder().id(2L).userId(7L).name("Farm").address("A").area(1d).build()));
        when(shipments.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getFarmShipmentDetail(2L, 99L));
    }

    @Test
    void getFarmShipmentDetail_rejectsShipmentNotOwnedByFarm() {
        loginFarmManager(7L);
        when(farms.findById(2L)).thenReturn(Optional.of(Farm.builder().id(2L).userId(7L).name("Farm").address("A").area(1d).build()));
        when(shipments.findById(10L)).thenReturn(Optional.of(shipment(10L, 5L, Shipment.STATUS_IN_TRANSIT)));
        // Farm's visible shipments do not include shipment #10
        when(shipments.findByFarmId(eq(2L), isNull())).thenReturn(List.of());
        assertThrows(ForbiddenException.class, () -> service.getFarmShipmentDetail(2L, 10L));
    }

    @Test
    void summary_countsByStatusAndComputesOnTimeRate() {
        loginFarmManager(7L);
        when(farms.findById(2L)).thenReturn(Optional.of(Farm.builder().id(2L).userId(7L).name("Farm").address("A").area(1d).build()));

        Shipment inTransit = shipment(1L, 11L, Shipment.STATUS_IN_TRANSIT);
        Shipment onTime = shipment(2L, 12L, Shipment.STATUS_DELIVERED);
        onTime.setDeliveryTime(LocalDateTime.of(2026, 8, 10, 9, 0));
        Shipment late = shipment(3L, 13L, Shipment.STATUS_DELIVERED);
        late.setDeliveryTime(LocalDateTime.of(2026, 8, 20, 9, 0));
        when(shipments.findByFarmId(eq(2L), isNull())).thenReturn(List.of(inTransit, onTime, late));

        Order o12 = new Order(); o12.setId(12L); o12.setDesiredDeliveryDate(LocalDate.of(2026, 8, 12));
        Order o13 = new Order(); o13.setId(13L); o13.setDesiredDeliveryDate(LocalDate.of(2026, 8, 15));
        when(orders.findById(12L)).thenReturn(Optional.of(o12));
        when(orders.findById(13L)).thenReturn(Optional.of(o13));

        FarmShipmentSummaryResponse s = service.getFarmShipmentSummary(2L);

        assertEquals(3, s.getTotal());
        assertEquals(1, s.getInTransit());
        assertEquals(2, s.getDelivered());
        assertEquals(1, s.getOnTimeDelivered());
        assertEquals(1, s.getLateDelivered());
        assertEquals(50.0, s.getOnTimeRatePercent(), 0.01);
    }
}
