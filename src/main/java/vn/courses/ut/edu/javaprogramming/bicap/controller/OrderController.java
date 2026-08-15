package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.dto.CreateDepositRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.DepositResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.OrderResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.RejectOrderRequest;
import vn.courses.ut.edu.javaprogramming.bicap.service.OrderService;

import java.util.List;

/**
 * Đơn hàng / yêu cầu mua nông sản (BICAP-20 / SRS-FM-014, detail-design §6.6).
 *
 * <p>Các endpoint xử lý yêu cầu mua (danh sách, chi tiết, chấp nhận, từ chối) là
 * portal endpoints — xác thực qua JWT ({@code CurrentUser}) và giới hạn FARM_MANAGER.
 * {@code POST /api/orders/deposit} (phía Retailer) giữ nguyên luồng đặt cọc hiện có.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** Danh sách yêu cầu mua trên các nông trại của Farm Manager (lọc theo trạng thái). */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(orderService.getFarmManagerOrders(status));
    }

    /** Chi tiết yêu cầu mua (Retailer, sản phẩm, số lượng, giá, thông tin cọc). */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderDetail(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderDetail(id));
    }

    /** Chấp nhận yêu cầu mua → PENDING → ACCEPTED (Retailer được thông báo). */
    @PutMapping("/{id}/accept")
    public ResponseEntity<OrderResponse> acceptOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.acceptOrder(id));
    }

    /** Từ chối yêu cầu mua → PENDING → REJECTED (bắt buộc nhập lý do). */
    @PutMapping("/{id}/reject")
    public ResponseEntity<OrderResponse> rejectOrder(
            @PathVariable Long id,
            @Valid @RequestBody RejectOrderRequest request) {
        return ResponseEntity.ok(orderService.rejectOrder(id, request.reason()));
    }

    @PostMapping("/deposit")
    public ResponseEntity<DepositResponse> createDeposit(
            @RequestHeader("X-Actor-Email") String actorEmail,
            @Valid @RequestBody CreateDepositRequest request) {
        return ResponseEntity.ok(orderService.createDeposit(request, actorEmail));
    }
}
