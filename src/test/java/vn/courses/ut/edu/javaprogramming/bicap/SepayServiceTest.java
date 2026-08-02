package vn.courses.ut.edu.javaprogramming.bicap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import vn.courses.ut.edu.javaprogramming.bicap.config.SepayConfig;
import vn.courses.ut.edu.javaprogramming.bicap.dto.SepayWebhookRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Subscription;
import vn.courses.ut.edu.javaprogramming.bicap.entity.SubscriptionStatus;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.OrderRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.PaymentRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.SubscriptionRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.OrderService;
import vn.courses.ut.edu.javaprogramming.bicap.service.SepayService;
import vn.courses.ut.edu.javaprogramming.bicap.service.SubscriptionService;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * H-8: the money path gets its first automated tests — webhook auth semantics,
 * account + amount validation, idempotency, and the state-machine guards.
 */
@ExtendWith(MockitoExtension.class)
class SepayServiceTest {

    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private OrderService orderService;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private OrderRepository orderRepository;

    private SepayConfig sepayConfig;
    private SepayService sepayService;

    @BeforeEach
    void setUp() {
        sepayConfig = new SepayConfig();
        sepayConfig.setAccountNo("123456789");
        sepayConfig.setBankName("MBBank");
        sepayService = new SepayService(subscriptionService, orderService, paymentRepository,
                subscriptionRepository, orderRepository, sepayConfig);
    }

    private SepayWebhookRequest request(String content, String account, long amount) {
        SepayWebhookRequest req = new SepayWebhookRequest();
        req.setContent(content);
        req.setAccountNumber(account);
        req.setTransferAmount(amount);
        req.setTransactionDate("2026-08-02T12:00:00");
        return req;
    }

    @Test
    void activatesSubscription_whenMemoMatches_whenAccountAndAmountAreValid() {
        Subscription sub = Subscription.builder()
                .id(5L).farmId(1L).packageId(1L)
                .status(SubscriptionStatus.PENDING_PAYMENT)
                .paymentCode("BICAP5001234")
                .build();
        when(subscriptionRepository.findByPaymentCode("BICAP5001234")).thenReturn(Optional.of(sub));
        when(paymentRepository.findByTxRef("BICAP5001234")).thenReturn(Optional.empty());

        Map<String, String> result = sepayService.handleWebhook(request("BICAP5001234", "123456789", 500_000));

        assertEquals("processed", result.get("message"));
        verify(subscriptionService).activateSubscription(5L, BigDecimal.valueOf(500_000));
        verify(paymentRepository).save(any());
    }

    @Test
    void rejectsWebhook_whenAccountNumberDoesNotMatch() {
        assertThrows(BadRequestException.class,
                () -> sepayService.handleWebhook(request("BICAP5001234", "999999999", 500_000)));
        verify(subscriptionService, never()).activateSubscription(anyLong(), any());
        verify(orderService, never()).markAsDepositPaid(anyLong(), any());
    }

    @Test
    void rejectsWebhook_whenContentIsMissing() {
        SepayWebhookRequest req = new SepayWebhookRequest();
        req.setAccountNumber("123456789");
        assertThrows(BadRequestException.class, () -> sepayService.handleWebhook(req));
    }

    @Test
    void skipsDuplicateSubscriptionPayment_idempotentOnTxRef() {
        Subscription sub = Subscription.builder()
                .id(5L).farmId(1L).packageId(1L)
                .status(SubscriptionStatus.PENDING_PAYMENT)
                .paymentCode("BICAP5001234")
                .build();
        when(subscriptionRepository.findByPaymentCode("BICAP5001234")).thenReturn(Optional.of(sub));
        when(paymentRepository.findByTxRef("BICAP5001234")).thenReturn(Optional.of(
                vn.courses.ut.edu.javaprogramming.bicap.entity.Payment.builder().txRef("BICAP5001234").build()));

        Map<String, String> result = sepayService.handleWebhook(request("BICAP5001234", "123456789", 500_000));

        assertEquals("duplicate", result.get("message"));
        verify(subscriptionService, never()).activateSubscription(anyLong(), any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void skipsDuplicateDepositPayment_idempotentOnTxRef() {
        Order order = new Order();
        order.setId(3L);
        order.setStatus(Order.STATUS_ACCEPTED);
        when(subscriptionRepository.findByPaymentCode("DEP3001234")).thenReturn(Optional.empty());
        when(orderRepository.findByDepositCode("DEP3001234")).thenReturn(Optional.of(order));
        when(paymentRepository.findByTxRef("DEP3001234")).thenReturn(Optional.of(
                vn.courses.ut.edu.javaprogramming.bicap.entity.Payment.builder().txRef("DEP3001234").build()));

        Map<String, String> result = sepayService.handleWebhook(request("DEP3001234", "123456789", 900_000));

        assertEquals("duplicate", result.get("message"));
        verify(orderService, never()).markAsDepositPaid(anyLong(), any());
    }

    @Test
    void marksDepositPaid_whenMemoMatchesOrder() {
        Order order = new Order();
        order.setId(3L);
        order.setStatus(Order.STATUS_ACCEPTED);
        when(subscriptionRepository.findByPaymentCode("DEP3001234")).thenReturn(Optional.empty());
        when(orderRepository.findByDepositCode("DEP3001234")).thenReturn(Optional.of(order));
        when(paymentRepository.findByTxRef("DEP3001234")).thenReturn(Optional.empty());

        Map<String, String> result = sepayService.handleWebhook(request("DEP3001234", "123456789", 900_000));

        assertEquals("processed", result.get("message"));
        verify(orderService).markAsDepositPaid(3L, BigDecimal.valueOf(900_000));
        verify(paymentRepository).save(any());
    }

    @Test
    void logsUnmatchedMemo_butReturnsSuccessForGateway() {
        when(subscriptionRepository.findByPaymentCode("UNKNOWN1234")).thenReturn(Optional.empty());
        when(orderRepository.findByDepositCode("UNKNOWN1234")).thenReturn(Optional.empty());

        Map<String, String> result = sepayService.handleWebhook(request("UNKNOWN1234", "123456789", 100));

        assertEquals("ignored", result.get("message"));
        verify(subscriptionService, never()).activateSubscription(anyLong(), any());
        verify(orderService, never()).markAsDepositPaid(anyLong(), any());
    }

    @Test
    void concurrentDuplicateSave_isCaughtByUniqueConstraintBackstop() {
        Subscription sub = Subscription.builder()
                .id(5L).farmId(1L).packageId(1L)
                .status(SubscriptionStatus.PENDING_PAYMENT)
                .paymentCode("BICAP5001234")
                .build();
        when(subscriptionRepository.findByPaymentCode("BICAP5001234")).thenReturn(Optional.of(sub));
        when(paymentRepository.findByTxRef("BICAP5001234")).thenReturn(Optional.empty());
        doThrow(new DataIntegrityViolationException("duplicate")).when(paymentRepository).save(any());

        // Must not propagate — the payment is treated as a duplicate.
        Map<String, String> result = sepayService.handleWebhook(request("BICAP5001234", "123456789", 500_000));

        assertEquals("processed", result.get("message"));
    }
}
