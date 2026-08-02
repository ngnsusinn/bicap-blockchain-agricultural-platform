package vn.courses.ut.edu.javaprogramming.bicap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.courses.ut.edu.javaprogramming.bicap.config.SepayConfig;
import vn.courses.ut.edu.javaprogramming.bicap.dto.CreateDepositRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.DepositResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Role;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.OrderRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.OrderService;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * H-3/H-5/H-8: deposit ownership (IDOR fix), persisted memo + amount, state machine.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    private SepayConfig sepayConfig;
    private User retailer;
    private Role retailerRole;

    @BeforeEach
    void setUp() {
        sepayConfig = new SepayConfig();
        sepayConfig.setAccountNo("123456789");
        sepayConfig.setBankName("MBBank");
        retailerRole = Role.builder().id(2L).name("RETAILER").permissions(new HashSet<>()).build();
        retailer = User.builder()
                .id(10L).email("retailer@bicap.com").password("x")
                .fullName("Retailer").status(UserStatus.ACTIVE).roles(Set.of(retailerRole))
                .build();
        // Rebuild with the real SepayConfig via constructor to keep the injected mocks.
        orderService = new OrderService(orderRepository, userRepository, sepayConfig);
    }

    private Order orderOwnedByRetailer() {
        Order order = new Order();
        order.setId(3L);
        order.setRetailerId(10L);
        order.setProductId(1L);
        order.setQuantity(100.0);
        order.setPrice(new BigDecimal("100000"));
        order.setDepositRate(0.3);
        order.setStatus(Order.STATUS_ACCEPTED);
        return order;
    }

    @Test
    void createDeposit_allowsOwner() {
        when(userRepository.findByEmail("retailer@bicap.com")).thenReturn(Optional.of(retailer));
        Order order = orderOwnedByRetailer();
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));
        when(orderRepository.findByDepositCode(any())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        DepositResponse response = orderService.createDeposit(new CreateDepositRequest(3L), "retailer@bicap.com");

        assertEquals(3L, response.getOrderId());
        assertNotNull(response.getPaymentCode());
        assertEquals(new BigDecimal("3000000.00"), response.getDepositAmount());
        // Persisted for webhook verification (M-8).
        assertEquals(response.getPaymentCode(), order.getDepositCode());
        assertEquals(new BigDecimal("3000000.00"), order.getDepositAmount());
    }

    @Test
    void createDeposit_forOtherUsersOrder_throwsForbidden() {
        when(userRepository.findByEmail("retailer@bicap.com")).thenReturn(Optional.of(retailer));
        Order order = orderOwnedByRetailer();
        order.setRetailerId(99L);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        assertThrows(ForbiddenException.class,
                () -> orderService.createDeposit(new CreateDepositRequest(3L), "retailer@bicap.com"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createDeposit_onPaidOrder_throwsBadRequest() {
        when(userRepository.findByEmail("retailer@bicap.com")).thenReturn(Optional.of(retailer));
        Order order = orderOwnedByRetailer();
        order.setStatus(Order.STATUS_DEPOSIT_PAID);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> orderService.createDeposit(new CreateDepositRequest(3L), "retailer@bicap.com"));
    }

    @Test
    void markAsDepositPaid_withInsufficientAmount_isRejected() {
        Order order = orderOwnedByRetailer();
        order.setDepositAmount(new BigDecimal("3000000.00"));
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> orderService.markAsDepositPaid(3L, new BigDecimal("1000")));
        assertEquals(Order.STATUS_ACCEPTED, order.getStatus());
    }

    @Test
    void markAsDepositPaid_withValidAmount_transitionsStatus() {
        Order order = orderOwnedByRetailer();
        order.setDepositAmount(new BigDecimal("3000000.00"));
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.markAsDepositPaid(3L, new BigDecimal("3000000.00"));

        assertEquals(Order.STATUS_DEPOSIT_PAID, order.getStatus());
    }

    @Test
    void markAsDepositPaid_onAlreadyPaidOrder_isRejected() {
        Order order = orderOwnedByRetailer();
        order.setStatus(Order.STATUS_DEPOSIT_PAID);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        assertThrows(vn.courses.ut.edu.javaprogramming.bicap.exception.ConflictException.class,
                () -> orderService.markAsDepositPaid(3L, new BigDecimal("3000000.00")));
        verify(orderRepository, never()).save(any());
    }
}
