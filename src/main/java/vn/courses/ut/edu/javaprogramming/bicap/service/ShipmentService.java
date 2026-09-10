package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.dto.OrderResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentCancelRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentCreateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentDetailResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.TrackingResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Driver;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Product;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Shipment;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Vehicle;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ConflictException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.DriverRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingSeasonRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.OrderRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ProductRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ShipmentRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ShipmentTrackingRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.VehicleRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shipping Manager operations: create/view/cancel shipments (BICAP-76).
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR1: Cancel only when status = PICKING_UP</li>
 *   <li>BR2: Driver must be IDLE</li>
 *   <li>BR3: Vehicle must be AVAILABLE</li>
 *   <li>BR4: Order must be in DEPOSIT_PAID state</li>
 * </ul>
 */
@Service
@Transactional
public class ShipmentService {

    private static final Set<String> SHIPPING_MGR_ROLES = Set.of("SHIPPING_MGR");

    private final ShipmentRepository        shipmentRepository;
    private final ShipmentTrackingRepository trackingRepository;
    private final OrderRepository           orderRepository;
    private final DriverRepository          driverRepository;
    private final VehicleRepository         vehicleRepository;
    private final UserRepository            userRepository;
    private final ProductRepository         productRepository;
    private final FarmingSeasonRepository   seasonRepository;
    private final FarmRepository            farmRepository;
    private final NotificationService       notificationService;

    public ShipmentService(ShipmentRepository shipmentRepository,
                           ShipmentTrackingRepository trackingRepository,
                           OrderRepository orderRepository,
                           DriverRepository driverRepository,
                           VehicleRepository vehicleRepository,
                           UserRepository userRepository,
                           ProductRepository productRepository,
                           FarmingSeasonRepository seasonRepository,
                           FarmRepository farmRepository,
                           NotificationService notificationService) {
        this.shipmentRepository  = shipmentRepository;
        this.trackingRepository  = trackingRepository;
        this.orderRepository     = orderRepository;
        this.driverRepository    = driverRepository;
        this.vehicleRepository   = vehicleRepository;
        this.userRepository      = userRepository;
        this.productRepository   = productRepository;
        this.seasonRepository    = seasonRepository;
        this.farmRepository      = farmRepository;
        this.notificationService = notificationService;
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    /**
     * Orders in DEPOSIT_PAID state awaiting shipment creation (BICAP-54).
     * Returns full OrderResponse so the frontend can display product/retailer/farm info.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getCompletedOrders() {
        requireShippingMgr();
        return orderRepository.findAll().stream()
                .filter(o -> Order.STATUS_DEPOSIT_PAID.equals(o.getStatus()))
                .map(this::buildOrderResponse)
                .toList();
    }

    /**
     * Driver reports (shipment_tracking entries with status REPORT_*) for SHIPPING_MGR (BICAP-62).
     * Optionally filtered by shipmentId.
     */
    @Transactional(readOnly = true)
    public List<TrackingResponse> getDriverReports(Long shipmentId) {
        requireShippingMgr();
        return trackingRepository.findDriverReports(shipmentId).stream()
                .map(TrackingResponse::from)
                .toList();
    }

    /**
     * Returns SHIP_DRIVER users that do not yet have a driver profile.
     * Used by the Shipping Manager to pick from existing accounts when creating a driver (BICAP-59).
     */
    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> getAvailableDriverUsers() {
        requireShippingMgr();
        // Get all user IDs already registered as drivers
        java.util.Set<Long> registeredIds = driverRepository.findAll().stream()
                .map(Driver::getUserId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());
        // Find users with SHIP_DRIVER role not yet in drivers table
        return userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> "SHIP_DRIVER".equals(r.getName())))
                .filter(u -> !registeredIds.contains(u.getId()))
                .map(u -> {
                    java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", u.getId());
                    m.put("fullName", u.getFullName());
                    m.put("email", u.getEmail());
                    m.put("phone", u.getPhone());
                    return m;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> getShipments(String status) {
        requireShippingMgr();
        String normalized = normalize(status);
        return shipmentRepository.findAllFiltered(normalized).stream()
                .map(s -> buildResponse(s))
                .toList();
    }

    @Transactional(readOnly = true)
    public ShipmentDetailResponse getShipmentDetail(Long id) {
        requireShippingMgr();
        return buildDetailResponse(findShipment(id));
    }

    // ── WRITE ─────────────────────────────────────────────────────────────────

    /**
     * Creates a shipment for a DEPOSIT_PAID order (BR4).
     * Assigns driver (BR2) and vehicle (BR3), then transitions:
     *  - Order → SHIPPING
     *  - Driver → ON_TRIP
     *  - Vehicle → IN_USE
     */
    public ShipmentResponse createShipment(ShipmentCreateRequest request) {
        requireShippingMgr();

        // BR4: order must be DEPOSIT_PAID
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));
        if (!Order.STATUS_DEPOSIT_PAID.equals(order.getStatus())) {
            throw new BadRequestException(
                    "Order must be in DEPOSIT_PAID state to create a shipment (current: " + order.getStatus() + ")");
        }

        // Prevent duplicate shipment for the same order
        if (shipmentRepository.findByOrderId(request.getOrderId()).isPresent()) {
            throw new ConflictException("A shipment already exists for order: " + request.getOrderId());
        }

        // BR2: driver must be IDLE
        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + request.getDriverId()));
        if (!Driver.STATUS_IDLE.equals(driver.getStatus())) {
            throw new BadRequestException(
                    "Driver must be IDLE to be assigned (current: " + driver.getStatus() + ")");
        }

        // BR3: vehicle must be AVAILABLE
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + request.getVehicleId()));
        if (!Vehicle.STATUS_AVAILABLE.equals(vehicle.getStatus())) {
            throw new BadRequestException(
                    "Vehicle must be AVAILABLE to be assigned (current: " + vehicle.getStatus() + ")");
        }

        // Create shipment
        Shipment shipment = new Shipment();
        shipment.setOrderId(request.getOrderId());
        shipment.setDriverId(request.getDriverId());
        shipment.setVehicleId(request.getVehicleId());
        shipment.setRouteSummary(request.getRouteSummary());
        Shipment saved = shipmentRepository.save(shipment);

        // State transitions
        order.setStatus(Order.STATUS_SHIPPING);
        orderRepository.save(order);

        driver.setStatus(Driver.STATUS_ON_TRIP);
        driverRepository.save(driver);

        vehicle.setStatus(Vehicle.STATUS_IN_USE);
        vehicleRepository.save(vehicle);

        // Notify the retailer that their order is now being shipped (BICAP-61)
        if (order.getRetailerId() != null) {
            notificationService.sendNotification(order.getRetailerId(), "INFO",
                    "Đơn hàng đang được vận chuyển",
                    "Đơn hàng #" + order.getId() + " đã được giao cho tài xế "
                            + userNameFor(driver.getUserId()) + " vận chuyển.",
                    false);
        }

        // Notify the Farm Manager that their goods are being picked up (BICAP-61)
        Product product = order.getProductId() != null
                ? productRepository.findById(order.getProductId()).orElse(null) : null;
        FarmingSeason season = (product != null && product.getSeasonId() != null)
                ? seasonRepository.findById(product.getSeasonId()).orElse(null) : null;
        Farm farm = (season != null && season.getFarmId() != null)
                ? farmRepository.findById(season.getFarmId()).orElse(null) : null;
        if (farm != null && farm.getUserId() != null) {
            notificationService.sendNotification(farm.getUserId(), "INFO",
                    "Hàng của bạn đang được vận chuyển",
                    "Sản phẩm từ đơn hàng #" + order.getId()
                            + " đang được tài xế " + userNameFor(driver.getUserId())
                            + " đến lấy hàng tại trang trại.",
                    false);
        }

        User driverUser = driver.getUserId() != null
                ? userRepository.findById(driver.getUserId()).orElse(null) : null;
        return ShipmentResponse.from(saved, order, driver, driverUser, vehicle);
    }

    /**
     * Cancels a shipment — BR1: only allowed when status = PICKING_UP.
     * Reverts driver → IDLE and vehicle → AVAILABLE, order → DEPOSIT_PAID.
     */
    public ShipmentResponse cancelShipment(Long id, ShipmentCancelRequest request) {
        requireShippingMgr();
        Shipment shipment = findShipment(id);

        if (!Shipment.STATUS_PICKING_UP.equals(shipment.getStatus())) {
            throw new BadRequestException(
                    "Shipment can only be cancelled when status is PICKING_UP (current: " + shipment.getStatus() + ")");
        }

        shipment.setStatus(Shipment.STATUS_RETURNED);
        shipmentRepository.save(shipment);

        // Revert order to DEPOSIT_PAID so SM can reassign
        orderRepository.findById(shipment.getOrderId()).ifPresent(order -> {
            order.setStatus(Order.STATUS_DEPOSIT_PAID);
            orderRepository.save(order);

            // Notify retailer
            if (order.getRetailerId() != null) {
                String reason = (request != null && request.getReason() != null)
                        ? ". Lý do: " + request.getReason() : ".";
                notificationService.sendNotification(order.getRetailerId(), "WARNING",
                        "Lô vận chuyển bị hủy",
                        "Lô vận chuyển đơn hàng #" + order.getId() + " đã bị hủy" + reason
                                + " Đơn hàng sẽ được sắp xếp vận chuyển lại.",
                        false);
            }
        });

        // Revert driver and vehicle
        if (shipment.getDriverId() != null) {
            driverRepository.findById(shipment.getDriverId()).ifPresent(d -> {
                d.setStatus(Driver.STATUS_IDLE);
                driverRepository.save(d);
            });
        }
        if (shipment.getVehicleId() != null) {
            vehicleRepository.findById(shipment.getVehicleId()).ifPresent(v -> {
                v.setStatus(Vehicle.STATUS_AVAILABLE);
                vehicleRepository.save(v);
            });
        }

        return buildResponse(shipment);
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private ShipmentResponse buildResponse(Shipment s) {
        Order order = s.getOrderId() != null
                ? orderRepository.findById(s.getOrderId()).orElse(null) : null;
        Driver driver = s.getDriverId() != null
                ? driverRepository.findById(s.getDriverId()).orElse(null) : null;
        User driverUser = (driver != null && driver.getUserId() != null)
                ? userRepository.findById(driver.getUserId()).orElse(null) : null;
        Vehicle vehicle = s.getVehicleId() != null
                ? vehicleRepository.findById(s.getVehicleId()).orElse(null) : null;
        return ShipmentResponse.from(s, order, driver, driverUser, vehicle);
    }

    private ShipmentDetailResponse buildDetailResponse(Shipment s) {
        Order order = s.getOrderId() != null
                ? orderRepository.findById(s.getOrderId()).orElse(null) : null;
        Driver driver = s.getDriverId() != null
                ? driverRepository.findById(s.getDriverId()).orElse(null) : null;
        User driverUser = (driver != null && driver.getUserId() != null)
                ? userRepository.findById(driver.getUserId()).orElse(null) : null;
        Vehicle vehicle = s.getVehicleId() != null
                ? vehicleRepository.findById(s.getVehicleId()).orElse(null) : null;
        List<TrackingResponse> tracking = trackingRepository
                .findByShipmentIdOrderByTimestampDesc(s.getId()).stream()
                .map(TrackingResponse::from)
                .toList();
        return ShipmentDetailResponse.fromDetail(s, order, driver, driverUser, vehicle, tracking);
    }

    private Shipment findShipment(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + id));
    }

    private String userNameFor(Long userId) {
        if (userId == null) return "N/A";
        return userRepository.findById(userId).map(User::getFullName).orElse("N/A");
    }

    /** Builds a full OrderResponse from an Order entity, null-safe for missing relations. */
    private OrderResponse buildOrderResponse(Order order) {
        Product product = order.getProductId() != null
                ? productRepository.findById(order.getProductId()).orElse(null) : null;
        FarmingSeason season = (product != null && product.getSeasonId() != null)
                ? seasonRepository.findById(product.getSeasonId()).orElse(null) : null;
        Farm farm = (season != null && season.getFarmId() != null)
                ? farmRepository.findById(season.getFarmId()).orElse(null) : null;
        User retailer = order.getRetailerId() != null
                ? userRepository.findById(order.getRetailerId()).orElse(null) : null;
        return OrderResponse.from(order, product, season, farm, retailer);
    }

    private static String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s.trim().toUpperCase();
    }

    private void requireShippingMgr() {
        User actor = CurrentUser.get();
        ActorAuthorizer.requireRoles(actor, SHIPPING_MGR_ROLES);
    }
}
