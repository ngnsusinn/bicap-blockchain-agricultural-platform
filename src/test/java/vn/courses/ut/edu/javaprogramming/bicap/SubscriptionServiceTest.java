package vn.courses.ut.edu.javaprogramming.bicap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import vn.courses.ut.edu.javaprogramming.bicap.config.SepayConfig;
import vn.courses.ut.edu.javaprogramming.bicap.dto.PurchasePackageRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.PurchasePackageResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Role;
import vn.courses.ut.edu.javaprogramming.bicap.entity.ServicePackage;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Subscription;
import vn.courses.ut.edu.javaprogramming.bicap.entity.SubscriptionStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ConflictException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ServicePackageRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.SubscriptionRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.SubscriptionService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * H-5/H-6/H-8: purchase ownership, state-machine activation, duplicate-subscription protection.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private ServicePackageRepository servicePackageRepository;
    @Mock
    private FarmRepository farmRepository;

    private SepayConfig sepayConfig;
    private SubscriptionService subscriptionService;

    private User farmOwner;
    private Role farmRole;
    private Farm farm;
    private ServicePackage servicePackage;

    @BeforeEach
    void setUp() {
        sepayConfig = new SepayConfig();
        sepayConfig.setAccountNo("123456789");
        sepayConfig.setBankName("MBBank");
        subscriptionService = new SubscriptionService(subscriptionRepository, servicePackageRepository,
                farmRepository, sepayConfig);

        farmRole = Role.builder().id(4L).name("FARM_MANAGER").permissions(new HashSet<>()).build();
        farmOwner = User.builder()
                .id(10L).email("farm@bicap.com").password("x")
                .fullName("Farm Owner").status(UserStatus.ACTIVE).roles(Set.of(farmRole))
                .build();
        farm = Farm.builder().id(1L).userId(10L).name("Trang Trại Xanh").address("Đồng Nai").area(12.5)
                .build();
        servicePackage = ServicePackage.builder().id(1L).name("Gói Vàng").price(new BigDecimal("500000"))
                .durationDays(30).status("ACTIVE").build();

        Authentication auth = new UsernamePasswordAuthenticationToken(farmOwner, null, farmOwner.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void purchasePackage_createsPendingSubscription_forOwnedFarm() {
        when(servicePackageRepository.findById(1L)).thenReturn(Optional.of(servicePackage));
        when(farmRepository.findByUserId(10L)).thenReturn(java.util.List.of(farm));
        when(subscriptionRepository.findByFarmIdAndStatus(1L, SubscriptionStatus.PENDING_PAYMENT)).thenReturn(Optional.empty());
        when(subscriptionRepository.findByFarmIdAndStatus(1L, SubscriptionStatus.ACTIVE)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(7L);
            return s;
        });
        when(subscriptionRepository.findByPaymentCode(any())).thenReturn(Optional.empty());

        PurchasePackageResponse response = subscriptionService.purchasePackage(new PurchasePackageRequest(1L));

        assertEquals(7L, response.getSubscriptionId());
        assertTrue(response.getPaymentCode().startsWith("BICAP7"));
        assertTrue(response.getPaymentCode().length() > 10);
    }

    @Test
    void purchasePackage_withoutOwnedFarm_throwsBadRequest() {
        when(servicePackageRepository.findById(1L)).thenReturn(Optional.of(servicePackage));
        when(farmRepository.findByUserId(10L)).thenReturn(java.util.List.of());

        assertThrows(vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException.class,
                () -> subscriptionService.purchasePackage(new PurchasePackageRequest(1L)));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void purchasePackage_whenPendingPaymentExists_throwsConflict() {
        when(servicePackageRepository.findById(1L)).thenReturn(Optional.of(servicePackage));
        when(farmRepository.findByUserId(10L)).thenReturn(java.util.List.of(farm));
        when(subscriptionRepository.findByFarmIdAndStatus(1L, SubscriptionStatus.ACTIVE)).thenReturn(Optional.empty());
        when(subscriptionRepository.findByFarmIdAndStatus(1L, SubscriptionStatus.PENDING_PAYMENT))
                .thenReturn(Optional.of(Subscription.builder().build()));

        assertThrows(ConflictException.class,
                () -> subscriptionService.purchasePackage(new PurchasePackageRequest(1L)));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void purchasePackage_expiresOldActiveSubscription_beforeCreatingNewPayment() {
        Subscription expiredByDate = Subscription.builder().id(6L).farmId(1L).packageId(1L)
                .status(SubscriptionStatus.ACTIVE).endDate(LocalDate.now().minusDays(1)).build();
        when(servicePackageRepository.findById(1L)).thenReturn(Optional.of(servicePackage));
        when(farmRepository.findByUserId(10L)).thenReturn(java.util.List.of(farm));
        when(subscriptionRepository.findByFarmIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(expiredByDate));
        when(subscriptionRepository.findByFarmIdAndStatus(1L, SubscriptionStatus.PENDING_PAYMENT)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription subscription = invocation.getArgument(0);
            if (subscription.getId() == null) subscription.setId(7L);
            return subscription;
        });
        when(subscriptionRepository.findByPaymentCode(any())).thenReturn(Optional.empty());

        subscriptionService.purchasePackage(new PurchasePackageRequest(1L));

        assertEquals(SubscriptionStatus.EXPIRED, expiredByDate.getStatus());
        verify(subscriptionRepository, atLeastOnce()).save(expiredByDate);
    }

    @Test
    void purchasePackage_onInactivePackage_throwsBadRequest() {
        servicePackage.setStatus("INACTIVE");
        when(servicePackageRepository.findById(1L)).thenReturn(Optional.of(servicePackage));

        assertThrows(vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException.class,
                () -> subscriptionService.purchasePackage(new PurchasePackageRequest(1L)));
    }

    @Test
    void activateSubscription_onlyFromPendingPayment() {
        Subscription sub = Subscription.builder().id(7L).farmId(1L).packageId(1L)
                .status(SubscriptionStatus.PENDING_PAYMENT).build();
        when(subscriptionRepository.findById(7L)).thenReturn(Optional.of(sub));
        when(servicePackageRepository.findById(1L)).thenReturn(Optional.of(servicePackage));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.activateSubscription(7L, new BigDecimal("500000"));

        assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus());
        assertNotNull(sub.getStartDate());
        assertNotNull(sub.getEndDate());
    }

    @Test
    void activateSubscription_alreadyActive_throwsConflict() {
        Subscription sub = Subscription.builder().id(7L).farmId(1L).packageId(1L)
                .status(SubscriptionStatus.ACTIVE).build();
        when(subscriptionRepository.findById(7L)).thenReturn(Optional.of(sub));

        assertThrows(ConflictException.class,
                () -> subscriptionService.activateSubscription(7L, new BigDecimal("500000")));
    }

    @Test
    void activateSubscription_insufficientAmount_isRejected() {
        Subscription sub = Subscription.builder().id(7L).farmId(1L).packageId(1L)
                .status(SubscriptionStatus.PENDING_PAYMENT).build();
        when(subscriptionRepository.findById(7L)).thenReturn(Optional.of(sub));
        when(servicePackageRepository.findById(1L)).thenReturn(Optional.of(servicePackage));

        assertThrows(vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException.class,
                () -> subscriptionService.activateSubscription(7L, new BigDecimal("100")));
        assertEquals(SubscriptionStatus.PENDING_PAYMENT, sub.getStatus());
    }

    @Test
    void checkPaymentStatus_throwsForbidden_forOtherUsersFarm() {
        Subscription sub = Subscription.builder().id(7L).farmId(2L).packageId(1L)
                .status(SubscriptionStatus.PENDING_PAYMENT).paymentCode("BICAP7000001").build();
        when(subscriptionRepository.findByPaymentCode("BICAP7000001")).thenReturn(Optional.of(sub));
        when(farmRepository.findByUserId(10L)).thenReturn(java.util.List.of(farm));

        assertThrows(ForbiddenException.class,
                () -> subscriptionService.checkPaymentStatus("BICAP7000001"));
    }
}
