package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.config.SepayConfig;
import vn.courses.ut.edu.javaprogramming.bicap.dto.SepayWebhookRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Payment;
import vn.courses.ut.edu.javaprogramming.bicap.entity.PaymentMethod;
import vn.courses.ut.edu.javaprogramming.bicap.entity.PaymentStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Subscription;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.OrderRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.PaymentRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.SubscriptionRepository;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Processes Sepay bank-transfer webhooks (C-3…C-5, M-8, M-9):
 * <ul>
 *   <li><b>Idempotency</b> — a repeated delivery for the same memo code is skipped (txRef dedup).</li>
 *   <li><b>Account check</b> — only transfers credited to the configured account are honored.</li>
 *   <li><b>Amount check</b> — performed inside {@link SubscriptionService#activateSubscription}
 *       / {@link OrderService#markAsDepositPaid} against the persisted expected amounts.</li>
 *   <li><b>Audit</b> — unmatched events are logged in full instead of silently swallowed.</li>
 * </ul>
 */
@Service
@Transactional
public class SepayService {

    private static final Logger log = LoggerFactory.getLogger(SepayService.class);

    private final SubscriptionService subscriptionService;
    private final OrderService orderService;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final OrderRepository orderRepository;
    private final SepayConfig sepayConfig;

    public SepayService(SubscriptionService subscriptionService,
                        OrderService orderService,
                        PaymentRepository paymentRepository,
                        SubscriptionRepository subscriptionRepository,
                        OrderRepository orderRepository,
                        SepayConfig sepayConfig) {
        this.subscriptionService = subscriptionService;
        this.orderService = orderService;
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.orderRepository = orderRepository;
        this.sepayConfig = sepayConfig;
    }

    /**
     * @return a JSON-friendly result map. {@code success=true} + {@code message=processed|ignored}
     *         is returned for events that match a known code (or are unrelated transfers).
     *         Auth/validation failures throw, which the controller maps to a non-2xx status.
     */
    public Map<String, String> handleWebhook(SepayWebhookRequest request) {
        if (request == null) {
            throw new BadRequestException("Empty webhook payload");
        }

        String content = request.getContent() != null ? request.getContent() : request.getDescription();
        if (content == null || content.trim().isEmpty()) {
            throw new BadRequestException("Webhook has no transfer content");
        }
        String memo = content.trim().toUpperCase(Locale.ROOT);

        // C-4 — verify the credited account is the configured one before trusting anything.
        String accountNumber = request.getAccountNumber();
        if (accountNumber == null || !accountNumber.equals(sepayConfig.getAccountNo())) {
            throw new BadRequestException("Credited account '" + accountNumber
                    + "' does not match the configured account '" + sepayConfig.getAccountNo() + "'");
        }

        BigDecimal amount = request.getTransferAmount() != null
                ? BigDecimal.valueOf(request.getTransferAmount())
                : BigDecimal.ZERO;

        // Subscription payment?
        Optional<Subscription> subscription = subscriptionRepository.findByPaymentCode(memo);
        if (subscription.isPresent()) {
            return handleSubscriptionPayment(subscription.get(), memo, amount);
        }

        // Deposit payment?
        Optional<Order> order = orderRepository.findByDepositCode(memo);
        if (order.isPresent()) {
            return handleDepositPayment(order.get(), memo, amount);
        }

        // C-3/M-9 — nothing matched: log the full event so no legitimate transfer is silently dropped.
        log.warn("Sepay webhook did not match any known transfer memo: id={}, code={}, amount={}, account={}, date={}",
                request.getId(), memo, amount, accountNumber, request.getTransactionDate());
        return result("ignored");
    }

    private Map<String, String> handleSubscriptionPayment(Subscription subscription, String memo, BigDecimal amount) {
        if (paymentRepository.findByTxRef(memo).isPresent()) {
            log.info("Duplicate subscription payment ignored: txRef={}", memo);
            return result("duplicate");
        }
        subscriptionService.activateSubscription(subscription.getId(), amount);
        savePayment(Payment.builder()
                .subscriptionId(subscription.getId())
                .amount(amount)
                .method(PaymentMethod.BANK_TRANSFER)
                .status(PaymentStatus.COMPLETED)
                .txRef(memo)
                .build());
        return result("processed");
    }

    private Map<String, String> handleDepositPayment(Order order, String memo, BigDecimal amount) {
        if (paymentRepository.findByTxRef(memo).isPresent()) {
            log.info("Duplicate deposit payment ignored: txRef={}", memo);
            return result("duplicate");
        }
        orderService.markAsDepositPaid(order.getId(), amount);
        savePayment(Payment.builder()
                .orderId(order.getId())
                .amount(amount)
                .method(PaymentMethod.BANK_TRANSFER)
                .status(PaymentStatus.COMPLETED)
                .txRef(memo)
                .build());
        return result("processed");
    }

    private void savePayment(Payment payment) {
        try {
            paymentRepository.save(payment);
        } catch (DataIntegrityViolationException e) {
            // Concurrent duplicate delivery slipped past the check — the unique txRef constraint
            // backs up the dedup. The payment is a replay, so failing the state change is correct.
            log.warn("Payment for txRef was saved concurrently; treating as duplicate", e);
        }
    }

    private Map<String, String> result(String message) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("success", "true");
        response.put("message", message);
        return response;
    }
}
