package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.dto.SepayWebhookRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Payment;
import vn.courses.ut.edu.javaprogramming.bicap.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class SepayService {

    private final SubscriptionService subscriptionService;
    private final OrderService orderService; // Thêm OrderService
    private final PaymentRepository paymentRepository;

    private static final Pattern SUB_CODE_PATTERN = Pattern.compile("BICAP\\d+");
    private static final Pattern DEP_CODE_PATTERN = Pattern.compile("DEP\\d+");

    public SepayService(SubscriptionService subscriptionService,
                        OrderService orderService,
                        PaymentRepository paymentRepository) {
        this.subscriptionService = subscriptionService;
        this.orderService = orderService;
        this.paymentRepository = paymentRepository;
    }

    public void handleWebhook(SepayWebhookRequest request) {
        String content = request.getContent();
        if (content == null) {
            content = request.getDescription();
        }
        if (content == null || content.isEmpty()) {
            return;
        }

        content = content.toUpperCase();
        Matcher subMatcher = SUB_CODE_PATTERN.matcher(content);
        Matcher depMatcher = DEP_CODE_PATTERN.matcher(content);

        if (subMatcher.find()) {
            String paymentCode = subMatcher.group();
            Long subscriptionId = subscriptionService.getSubscriptionIdByPaymentCode(paymentCode);
            if (subscriptionId != null) {
                Payment payment = Payment.builder()
                        .amount(request.getTransferAmount() != null
                                ? BigDecimal.valueOf(request.getTransferAmount())
                                : BigDecimal.ZERO)
                        .method("BANK_TRANSFER")
                        .status("SUCCESS")
                        .txRef(paymentCode)
                        .orderId(subscriptionId)
                        .createdAt(LocalDateTime.now())
                        .build();
                paymentRepository.save(payment);

                subscriptionService.activateSubscription(subscriptionId);
            }
        } 
        else if (depMatcher.find()) {
            String depositCode = depMatcher.group();
            Long orderId = orderService.getOrderIdByDepositCode(depositCode);
            if (orderId != null) {
                Payment payment = Payment.builder()
                        .amount(request.getTransferAmount() != null
                                ? BigDecimal.valueOf(request.getTransferAmount())
                                : BigDecimal.ZERO)
                        .method("BANK_TRANSFER")
                        .status("SUCCESS")
                        .txRef(depositCode)
                        .orderId(orderId)
                        .createdAt(LocalDateTime.now())
                        .build();
                paymentRepository.save(payment);

                orderService.markAsDepositPaid(orderId);
            }
        }
    }
}