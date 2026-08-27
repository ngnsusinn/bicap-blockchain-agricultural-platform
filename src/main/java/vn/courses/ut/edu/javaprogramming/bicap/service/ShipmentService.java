package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentCancelRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentCreateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentDetailResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.TrackingResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Driver;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Shipment;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Vehicle;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ConflictException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.DriverRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.OrderRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ShipmentRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ShipmentTrackingRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.VehicleRepository;

import java.util.List;
import java.util.Set;

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
    private final NotificationService       notificationService;

    public ShipmentService(ShipmentRepository shipmentRepository,
                           ShipmentTrackingRepository trackingRepository,
                           OrderRepository orderRepository,
                           DriverRepository driverRepository,
                           VehicleRepository vehicleRepository,
                           UserRepository userRepository,
                           NotificationService notificationService) {
        this.shipmentRepository  = shipmentRepository;
        this.trackingRepository  = trackingRepository;
        this.orderRepository     = orderRepository;
        this.driverRepository    = driverRepository;
        this.vehicleRepository   = vehicleRepository;
        this.userRepository      = userRepository;
        this.notificationService = notificationService;
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    /** Orders in DEPOSIT_PAID state awaiting shipment creation. */
    @Transactional(readOnly = true)
    public List<Order> getCompletedOrders() {
        requireShippingMgr();
        return orderRepository.findAll().stream()
                .filter(o -> Order.STATUS_DEPOSIT_PAID.equals(o.getStatus()))
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

        // Notify the retailer that their order is now being shipped
        if (order.getRetailerId() != null) {
            notificationService.sendNotification(order.getRetailerId(), "INFO",
                    "Đơn hàng đang được vận chuyển",
                    "Đơn hàng #" + order.getId() + " đã được giao cho tài xế "
                            + userNameFor(driver.getUserId()) + " vận chuyển.",
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

    private static String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s.trim().toUpperCase();
    }

    private void requireShippingMgr() {
        User actor = CurrentUser.get();
        ActorAuthorizer.requireRoles(actor, SHIPPING_MGR_ROLES);
    }
}
