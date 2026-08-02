package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.config.SepayConfig;
import vn.courses.ut.edu.javaprogramming.bicap.dto.CreateDepositRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.DepositResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ConflictException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.OrderRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.Set;

@Service
@Transactional
public class OrderService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DEPOSIT_CODE_DIGITS = 6;

    private static final Set<String> DEPOSITABLE_STATUSES =
            Set.of(Order.STATUS_PENDING, Order.STATUS_ACCEPTED);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final SepayConfig sepayConfig;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository, SepayConfig sepayConfig) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.sepayConfig = sepayConfig;
    }

    /**
     * Creates a deposit transfer memo for an order the CURRENT user owns (H-3 — no more
     * IDOR: any authenticated user can no longer read another party's deposit details).
     * The memo code and expected amount are persisted on the order so the webhook can
     * verify and dedup them (M-8).
     */
    public DepositResponse createDeposit(CreateDepositRequest request, String actorEmail) {
        User actor = ActorAuthorizer.requireActor(userRepository, actorEmail);

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getRetailerId() == null || !order.getRetailerId().equals(actor.getId())) {
            throw new ForbiddenException("This order does not belong to the current user");
        }

        if (!DEPOSITABLE_STATUSES.contains(order.getStatus())) {
            throw new BadRequestException("Order is not in a valid state for deposit (current: " + order.getStatus() + ")");
        }

        // Tỷ lệ đặt cọc (30% = 0.3) — computed in BigDecimal, rounded to 2 decimals (no double drift).
        BigDecimal rate = order.getDepositRate() != null
                ? BigDecimal.valueOf(order.getDepositRate())
                : new BigDecimal("0.3");
        BigDecimal totalAmount = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
        BigDecimal depositAmount = totalAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);

        // Sinh mã chuyển khoản cọc dạng DEP{orderId}{6_số_ngẫu_nhiên} — SecureRandom, persisted & unique.
        String depositCode = uniqueDepositCode(order.getId());
        order.setDepositCode(depositCode);
        order.setDepositAmount(depositAmount);
        orderRepository.save(order);

        return new DepositResponse(
                order.getId(),
                depositCode,
                sepayConfig.getBankName(),
                sepayConfig.getAccountNo(),
                depositAmount,
                depositCode
        );
    }

    /**
     * Marks an order DEPOSIT_PAID after a verified transfer. State-machine guarded (H-5):
     * only PENDING/ACCEPTED may transition, the amount must meet the persisted deposit,
     * and an already-paid order cannot be re-marked.
     */
    public void markAsDepositPaid(Long orderId, BigDecimal transferAmount) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (Order.STATUS_DEPOSIT_PAID.equals(order.getStatus())) {
            throw new ConflictException("Order has already been marked as paid");
        }        if (!DEPOSITABLE_STATUSES.contains(order.getStatus())) {
            throw new BadRequestException("Order is not in a valid state for deposit payment (current: "
                    + order.getStatus() + ")");
        }

        BigDecimal expected = order.getDepositAmount();
        if (expected == null) {
            throw new BadRequestException("No deposit has been initiated for this order");
        }
        if (transferAmount == null || transferAmount.compareTo(expected) < 0) {
            throw new BadRequestException("Transferred amount is below the required deposit");
        }

        order.setStatus(Order.STATUS_DEPOSIT_PAID);
        orderRepository.save(order);
    }

    private String uniqueDepositCode(Long orderId) {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = "DEP" + orderId + String.format("%0" + DEPOSIT_CODE_DIGITS + "d", RANDOM.nextInt(1_000_000));
            if (orderRepository.findByDepositCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique deposit code");
    }
}
