package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.dto.CancelOrderRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.CreateDepositRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.DepositResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.OrderResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.PlaceOrderRequest;
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
    // ── BICAP-75: Retailer đặt mua, hủy đơn, hoàn thành; Farm Manager giao hàng ──
    /** Retailer đặt mua nông sản mới → tạo đơn với status PENDING. */
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.ok(orderService.placeOrder(request));
    }
    /** Retailer xem danh sách đơn hàng của chính mình, lọc theo trạng thái. */
    @GetMapping("/my")
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(orderService.getRetailerOrders(status));
    }
    /** Retailer hủy đơn (chỉ khi PENDING hoặc ACCEPTED). */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CancelOrderRequest request) {
        return ResponseEntity.ok(orderService.cancelOrder(id, request));
    }
    /** Farm Manager xác nhận đã giao hàng (DEPOSIT_PAID → DELIVERED). */
    @PutMapping("/{id}/deliver")
    public ResponseEntity<OrderResponse> confirmDelivery(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.confirmDelivery(id));
    }
    /** Retailer xác nhận đã nhận hàng (DELIVERED → COMPLETED). */
    @PutMapping("/{id}/complete")
    public ResponseEntity<OrderResponse> completeOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.completeOrder(id));
    }
}
