package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.dto.DriverResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.OrderResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentCreateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.VehicleResponse;
import vn.courses.ut.edu.javaprogramming.bicap.service.ShipmentService;

import java.util.List;
import java.util.Map;

/**
 * REST Controller cho module Shipping.
 * Base URL: /api/shipping
 *
 * <ul>
 *   <li>BICAP-54 — GET  /api/shipping/orders/completed  — Đơn hàng chờ vận chuyển</li>
 *   <li>BICAP-55 — POST /api/shipping/shipments          — Tạo lô vận chuyển</li>
 *   <li>BICAP-56 — PUT  /api/shipping/shipments/{id}/cancel — Hủy lô vận chuyển</li>
 *   <li>BICAP-57 — GET  /api/shipping/shipments/{id}     — Chi tiết lô + tracking</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/shipping")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    /**
     * BICAP-54: Danh sách đơn hàng đã thanh toán cọc, chưa có lô vận chuyển.
     * Endpoint: GET /api/shipping/orders/completed
     */
    @GetMapping("/orders/completed")
    @PreAuthorize("hasRole('SHIPPING_MGR')")
    public ResponseEntity<List<OrderResponse>> getCompletedOrders() {
        return ResponseEntity.ok(shipmentService.getCompletedOrders());
    }

    /**
     * BICAP-55: Tạo lô vận chuyển mới.
     * Endpoint: POST /api/shipping/shipments
     */
    @PostMapping("/shipments")
    @PreAuthorize("hasRole('SHIPPING_MGR')")
    public ResponseEntity<ShipmentResponse> createShipment(
            @Valid @RequestBody ShipmentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shipmentService.createShipment(request));
    }

    /**
     * Danh sách tất cả lô vận chuyển.
     * Endpoint: GET /api/shipping/shipments
     */
    @GetMapping("/shipments")
    @PreAuthorize("hasRole('SHIPPING_MGR')")
    public ResponseEntity<List<ShipmentResponse>> getAllShipments() {
        return ResponseEntity.ok(shipmentService.getAllShipments());
    }

    /**
     * BICAP-57: Chi tiết lô vận chuyển kèm lịch sử tracking.
     * Endpoint: GET /api/shipping/shipments/{id}
     */
    @GetMapping("/shipments/{id}")
    @PreAuthorize("hasRole('SHIPPING_MGR') or hasRole('SHIP_DRIVER')")
    public ResponseEntity<ShipmentResponse> getShipmentDetail(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.getShipmentDetail(id));
    }

    /**
     * BICAP-56: Hủy lô vận chuyển (chỉ khi status = PICKING_UP).
     * Endpoint: PUT /api/shipping/shipments/{id}/cancel
     */
    @PutMapping("/shipments/{id}/cancel")
    @PreAuthorize("hasRole('SHIPPING_MGR')")
    public ResponseEntity<Map<String, String>> cancelShipment(@PathVariable Long id) {
        shipmentService.cancelShipment(id);
        return ResponseEntity.ok(Map.of("message", "Lô vận chuyển đã được hủy thành công."));
    }

    /**
     * GET /api/shipping/vehicles — Danh sách phương tiện (dùng cho form tạo lô).
     */
    @GetMapping("/vehicles")
    @PreAuthorize("hasRole('SHIPPING_MGR')")
    public ResponseEntity<List<VehicleResponse>> getVehicles() {
        return ResponseEntity.ok(shipmentService.getVehicles());
    }

    /**
     * GET /api/shipping/drivers — Danh sách tài xế (dùng cho form tạo lô).
     */
    @GetMapping("/drivers")
    @PreAuthorize("hasRole('SHIPPING_MGR')")
    public ResponseEntity<List<DriverResponse>> getDrivers() {
        return ResponseEntity.ok(shipmentService.getDrivers());
    }
}
