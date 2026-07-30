package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.config.SepayConfig;
import vn.courses.ut.edu.javaprogramming.bicap.dto.CreateDepositRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.DepositResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.Random;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final SepayConfig sepayConfig;

    public OrderService(OrderRepository orderRepository, SepayConfig sepayConfig) {
        this.orderRepository = orderRepository;
        this.sepayConfig = sepayConfig;
    }

    public DepositResponse createDeposit(CreateDepositRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!"ACCEPTED".equals(order.getStatus()) && !"PENDING".equals(order.getStatus())) {
            throw new BadRequestException("Order is not in a valid state for deposit");
        }

        // Tỷ lệ đặt cọc (30% = 0.3)
        double rate = order.getDepositRate() != null ? order.getDepositRate() : 0.3;
        BigDecimal totalAmount = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
        BigDecimal depositAmount = totalAmount.multiply(BigDecimal.valueOf(rate));

        // Sinh mã chuyển khoản cọc dạng DEP{orderId}{4_số_ngẫu_nhiên}
        String depositCode = "DEP" + order.getId() + String.format("%04d", new Random().nextInt(10000));

        return new DepositResponse(
                order.getId(),
                depositCode,
                sepayConfig.getBankName(),
                sepayConfig.getAccountNo(),
                depositAmount,
                depositCode
        );
    }

    public Long getOrderIdByDepositCode(String depositCode) {
        if (depositCode == null || !depositCode.startsWith("DEP") || depositCode.length() <= 7) {
            return null;
        }
        try {
            String idStr = depositCode.substring(3, depositCode.length() - 4);
            return Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void markAsDepositPaid(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        order.setStatus("DEPOSIT_PAID");
        orderRepository.save(order);
    }
}