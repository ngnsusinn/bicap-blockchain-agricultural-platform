package vn.courses.ut.edu.javaprogramming.bicap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Role;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.BlockchainTransactionRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.OrderRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ProductRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ReportRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.AdminDashboardService;
import vn.courses.ut.edu.javaprogramming.bicap.service.FarmApprovalService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * EPIC-1 dashboard — AdminDashboardService tổng hợp số liệu và chỉ mở cho ADMIN_VIEW.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class AdminDashboardServiceTest {

    @Mock UserRepository users;
    @Mock FarmRepository farms;
    @Mock ProductRepository products;
    @Mock OrderRepository orders;
    @Mock ReportRepository reports;
    @Mock BlockchainTransactionRepository blockchain;
    @Mock FarmApprovalService farmApprovalService;
    AdminDashboardService service;

    private void init() { service = new AdminDashboardService(users, farms, products, orders, reports, blockchain, farmApprovalService); }

    private User admin() {
        Role role = Role.builder().name("ADMIN").permissions(Set.of()).build();
        return User.builder().id(1L).email("admin@bicap.com").status(UserStatus.ACTIVE).roles(Set.of(role)).build();
    }

    @Test
    void dashboard_aggregatesAllCounters() {
        init();
        when(users.findByEmail("admin@bicap.com")).thenReturn(Optional.of(admin()));
        when(users.findDistinctByRoles_NameIn(anyCollection())).thenReturn(List.of(admin(), admin()));
        when(farms.countByStatus(any(FarmStatus.class))).thenReturn(1L);
        when(farms.count()).thenReturn(4L);
        when(products.countByStatus(anyString())).thenReturn(2L);
        when(products.count()).thenReturn(6L);
        when(orders.countGroupedByStatus()).thenReturn(List.of(new Object[]{"PENDING", 3L}, new Object[]{"COMPLETED", 5L}));
        when(orders.count()).thenReturn(8L);
        when(reports.countByStatus(anyString())).thenReturn(1L);
        when(reports.count()).thenReturn(4L);
        Page<FarmResponse> pendingPage = new PageImpl<>(List.of());
        when(farmApprovalService.getFarms(eq(FarmStatus.PENDING), isNull(), any(Pageable.class), eq("admin@bicap.com")))
                .thenReturn(pendingPage);
        when(blockchain.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        Map<String, Object> result = service.getDashboard("admin@bicap.com");

        assertEquals(2, result.get("admins"));
        assertEquals(4L, ((Map<?, ?>) result.get("farms")).get("TOTAL"));
        assertEquals(6L, ((Map<?, ?>) result.get("products")).get("TOTAL"));
        assertEquals(5L, ((Map<?, ?>) result.get("orders")).get("COMPLETED"));
        assertEquals(4L, ((Map<?, ?>) result.get("reports")).get("TOTAL"));
        assertNotNull(result.get("pendingFarms"));
        assertNotNull(result.get("recentTransactions"));
    }

    @Test
    void dashboard_forbidsNonAdminActor() {
        init();
        Role farmRole = Role.builder().name("FARM_MANAGER").permissions(Set.of()).build();
        User farmer = User.builder().id(9L).email("farm@bicap.com").status(UserStatus.ACTIVE).roles(Set.of(farmRole)).build();
        when(users.findByEmail("farm@bicap.com")).thenReturn(Optional.of(farmer));

        assertThrows(ForbiddenException.class, () -> service.getDashboard("farm@bicap.com"));
    }
}
