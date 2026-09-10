package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentDetailResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Product;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingSeasonRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.OrderRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ProductRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.NotificationService;
import vn.courses.ut.edu.javaprogramming.bicap.service.RetailerShipmentService;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Retailer endpoints for shipment tracking (BICAP-49 / SRS-RT-014) and
 * sending messages to a Farm Manager (BICAP-48 / SRS-RT-013).
 *
 * <p>All routes require RETAILER role — enforced via CurrentUser + ActorAuthorizer.
 */
@RestController
@RequestMapping("/api/retailer")
public class RetailerController {

    private static final Set<String> RETAILER_ROLES = Set.of("RETAILER");

    private final RetailerShipmentService retailerShipmentService;
    private final NotificationService notificationService;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final FarmingSeasonRepository seasonRepository;
    private final FarmRepository farmRepository;

    public RetailerController(RetailerShipmentService retailerShipmentService,
                               NotificationService notificationService,
                               OrderRepository orderRepository,
                               ProductRepository productRepository,
                               FarmingSeasonRepository seasonRepository,
                               FarmRepository farmRepository) {
        this.retailerShipmentService = retailerShipmentService;
        this.notificationService = notificationService;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.seasonRepository = seasonRepository;
        this.farmRepository = farmRepository;
    }

    // ── BICAP-49: Xem quy trình vận chuyển ───────────────────────────────────

    /**
     * Danh sách lô vận chuyển liên quan đến các đơn của retailer hiện tại.
     * Hỗ trợ lọc theo status (PICKING_UP, IN_TRANSIT, DELIVERED, RETURNED).
     */
    @GetMapping("/shipments")
    public ResponseEntity<List<ShipmentResponse>> getMyShipments(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(retailerShipmentService.getMyShipments(status));
    }

    /**
     * Chi tiết một lô vận chuyển kèm lịch sử GPS.
     * Chỉ trả về nếu shipment thuộc đơn hàng của retailer đang đăng nhập.
     */
    @GetMapping("/shipments/{id}")
    public ResponseEntity<ShipmentDetailResponse> getShipmentDetail(@PathVariable Long id) {
        return ResponseEntity.ok(retailerShipmentService.getMyShipmentDetail(id));
    }

    // ── BICAP-48: Gửi thông báo cho Farm Manager ─────────────────────────────

    /**
     * Retailer gửi tin nhắn/thông báo đến Farm Manager của một đơn hàng cụ thể.
     *
     * <p>Cơ chế: lấy Farm Manager từ đơn hàng (order → product → season → farm → userId),
     * sau đó dùng NotificationService để gửi thông báo in-app (SSE + persist).
     *
     * <p>Request body:
     * <pre>
     * {
     *   "orderId": 123,         // bắt buộc — phải thuộc đơn của retailer hiện tại
     *   "message": "..."        // bắt buộc, 1-1000 ký tự
     * }
     * </pre>
     */
    @PostMapping("/notify-farm")
    public ResponseEntity<Map<String, String>> notifyFarm(
            @Valid @RequestBody NotifyFarmRequest request) {

        User actor = CurrentUser.get();
        ActorAuthorizer.requireRoles(actor, RETAILER_ROLES);

        // 1. Load order và kiểm tra ownership
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.orderId()));

        if (!actor.getId().equals(order.getRetailerId())) {
            throw new ForbiddenException("This order does not belong to the current user");
        }

        // 2. Resolve Farm Manager userId: order → product → season → farm → userId
        Product product = order.getProductId() != null
                ? productRepository.findById(order.getProductId()).orElse(null) : null;
        FarmingSeason season = product != null && product.getSeasonId() != null
                ? seasonRepository.findById(product.getSeasonId()).orElse(null) : null;
        Farm farm = season != null && season.getFarmId() != null
                ? farmRepository.findById(season.getFarmId()).orElse(null) : null;

        if (farm == null || farm.getUserId() == null) {
            throw new BadRequestException("Cannot resolve Farm Manager for this order");
        }

        // 3. Gửi thông báo in-app cho Farm Manager
        notificationService.sendNotification(
                farm.getUserId(),
                "INFO",
                "Tin nhắn từ Nhà bán lẻ",
                actor.getFullName() + " (đơn #" + order.getId() + "): " + request.message().trim(),
                false
        );

        return ResponseEntity.ok(Map.of("message", "Thông báo đã được gửi đến Farm Manager."));
    }

    // ── Inner record for notify-farm request body ─────────────────────────────

    record NotifyFarmRequest(
            Long orderId,

            @NotBlank(message = "Message must not be blank")
            @Size(min = 1, max = 1000, message = "Message must be between 1 and 1000 characters")
            String message) {
    }
}
