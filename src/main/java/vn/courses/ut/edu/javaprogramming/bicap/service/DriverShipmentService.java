package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.common.util.ImagesJson;
import vn.courses.ut.edu.javaprogramming.bicap.dto.DeliveryConfirmRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.DriverReportRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.PickupConfirmRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentDetailResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.TrackingAddRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.TrackingResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Driver;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Shipment;
import vn.courses.ut.edu.javaprogramming.bicap.entity.ShipmentTracking;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Vehicle;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.DriverRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.OrderRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ShipmentRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ShipmentTrackingRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.VehicleRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Driver-facing shipment operations for the mobile app (BICAP-76).
 *
 * <p>Every method enforces that the calling driver owns the shipment (BR5).
 * BR4: GPS tracking updates must be within a reasonable distance (≤ 500 km)
 *      from the previous checkpoint to reject obviously bogus coordinates.
 */
@Service
@Transactional
public class DriverShipmentService {

    /** BR4: Maximum allowed distance (km) between consecutive tracking points. */
    private static final double MAX_TRACKING_DISTANCE_KM = 500.0;

    private static final Set<String> SHIP_DRIVER_ROLES = Set.of("SHIP_DRIVER");

    private final ShipmentRepository        shipmentRepository;
    private final ShipmentTrackingRepository trackingRepository;
    private final OrderRepository           orderRepository;
    private final DriverRepository          driverRepository;
    private final VehicleRepository         vehicleRepository;
    private final UserRepository            userRepository;
    private final NotificationService       notificationService;

    public DriverShipmentService(ShipmentRepository shipmentRepository,
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

    @Transactional(readOnly = true)
    public List<ShipmentResponse> getMyShipments(String status) {
        Driver driver = requireDriverProfile();
        String normalized = normalize(status);
        return shipmentRepository.findByDriverIdFiltered(driver.getId(), normalized).stream()
                .map(s -> buildResponse(s, driver))
                .toList();
    }

    @Transactional(readOnly = true)
    public ShipmentDetailResponse getShipmentDetail(Long shipmentId) {
        Driver driver = requireDriverProfile();
        Shipment shipment = findOwnedShipment(shipmentId, driver.getId());
        return buildDetailResponse(shipment, driver);
    }

    // ── WRITE ─────────────────────────────────────────────────────────────────

    /** Driver adds a GPS checkpoint during transit (both PICKING_UP and IN_TRANSIT are allowed). */
    public TrackingResponse addTracking(Long shipmentId, TrackingAddRequest request) {
        Driver driver = requireDriverProfile();
        Shipment shipment = findOwnedShipment(shipmentId, driver.getId());

        if (Shipment.STATUS_DELIVERED.equals(shipment.getStatus())
                || Shipment.STATUS_RETURNED.equals(shipment.getStatus())) {
            throw new BadRequestException("Cannot add tracking to a completed shipment (status: " + shipment.getStatus() + ")");
        }

        // BR4: GPS coordinates must be within reasonable distance from the previous checkpoint
        List<ShipmentTracking> existing = trackingRepository
                .findByShipmentIdOrderByTimestampDesc(shipmentId);
        if (!existing.isEmpty()) {
            ShipmentTracking last = existing.get(0);
            double distanceKm = haversineKm(last.getGpsLat(), last.getGpsLng(),
                    request.getGpsLat(), request.getGpsLng());
            if (distanceKm > MAX_TRACKING_DISTANCE_KM) {
                throw new BadRequestException(
                        String.format("BR4: GPS update is %.1f km away from the last checkpoint — " +
                                "maximum allowed is %.0f km. Please verify coordinates.",
                                distanceKm, MAX_TRACKING_DISTANCE_KM));
            }
        }

        ShipmentTracking tracking = new ShipmentTracking();
        tracking.setShipmentId(shipmentId);
        tracking.setStatus(request.getStatus());
        tracking.setGpsLat(request.getGpsLat());
        tracking.setGpsLng(request.getGpsLng());
        tracking.setImages(ImagesJson.toJson(request.getImages()));
        tracking.setNotes(request.getNotes());

        return TrackingResponse.from(trackingRepository.save(tracking));
    }

    /**
     * Driver confirms pickup at the farm (PICKING_UP → IN_TRANSIT).
     * Records a tracking checkpoint and sets pickupTime.
     */
    public ShipmentDetailResponse confirmPickup(Long shipmentId, PickupConfirmRequest request) {
        Driver driver = requireDriverProfile();
        Shipment shipment = findOwnedShipment(shipmentId, driver.getId());

        if (!Shipment.STATUS_PICKING_UP.equals(shipment.getStatus())) {
            throw new BadRequestException(
                    "Shipment must be in PICKING_UP state to confirm pickup (current: " + shipment.getStatus() + ")");
        }

        // Persist tracking checkpoint
        ShipmentTracking tracking = new ShipmentTracking();
        tracking.setShipmentId(shipmentId);
        tracking.setStatus("PICKUP_CONFIRMED");
        tracking.setGpsLat(request.getGpsLat());
        tracking.setGpsLng(request.getGpsLng());
        tracking.setImages(ImagesJson.toJson(request.getImages()));
        tracking.setNotes(request.getNotes());
        trackingRepository.save(tracking);

        shipment.setStatus(Shipment.STATUS_IN_TRANSIT);
        shipment.setPickupTime(LocalDateTime.now());
        shipmentRepository.save(shipment);

        return buildDetailResponse(shipment, driver);
    }

    /**
     * Driver confirms successful delivery (IN_TRANSIT → DELIVERED).
     * Transitions:
     *  - Shipment → DELIVERED
     *  - Order → DELIVERED
     *  - Driver → IDLE
     *  - Vehicle → AVAILABLE
     */
    public ShipmentDetailResponse confirmDelivery(Long shipmentId, DeliveryConfirmRequest request) {
        Driver driver = requireDriverProfile();
        Shipment shipment = findOwnedShipment(shipmentId, driver.getId());

        if (!Shipment.STATUS_IN_TRANSIT.equals(shipment.getStatus())) {
            throw new BadRequestException(
                    "Shipment must be IN_TRANSIT to confirm delivery (current: " + shipment.getStatus() + ")");
        }

        // Delivery tracking checkpoint
        ShipmentTracking tracking = new ShipmentTracking();
        tracking.setShipmentId(shipmentId);
        tracking.setStatus("DELIVERY_CONFIRMED");
        tracking.setGpsLat(request.getGpsLat());
        tracking.setGpsLng(request.getGpsLng());
        tracking.setImages(ImagesJson.toJson(request.getImages()));
        tracking.setNotes(request.getNotes());
        trackingRepository.save(tracking);

        shipment.setStatus(Shipment.STATUS_DELIVERED);
        shipment.setDeliveryTime(LocalDateTime.now());
        shipmentRepository.save(shipment);

        // Revert driver and vehicle
        driver.setStatus(Driver.STATUS_IDLE);
        driverRepository.save(driver);

        if (shipment.getVehicleId() != null) {
            vehicleRepository.findById(shipment.getVehicleId()).ifPresent(v -> {
                v.setStatus(Vehicle.STATUS_AVAILABLE);
                vehicleRepository.save(v);
            });
        }

        // Transition order to DELIVERED and notify retailer
        orderRepository.findById(shipment.getOrderId()).ifPresent(order -> {
            order.setStatus(Order.STATUS_DELIVERED);
            order.setDeliveredAt(LocalDateTime.now());
            orderRepository.save(order);

            if (order.getRetailerId() != null) {
                notificationService.sendNotification(order.getRetailerId(), "SUCCESS",
                        "Đơn hàng đã được giao",
                        "Đơn hàng #" + order.getId()
                                + " đã được giao thành công. Vui lòng xác nhận đã nhận hàng.",
                        false);
            }
        });


        return buildDetailResponse(shipment, driver);
    }

    // ── DRIVER REPORT (BICAP-76 / detail-design §2.7 line 881) ───────────────

    /**
     * Driver sends an incident / delay / damage report to the Shipping Manager.
     * Persists a tracking point with the report details and notifies the SM via notification.
     */
    public TrackingResponse sendReport(DriverReportRequest request) {
        Driver driver = requireDriverProfile();

        // Validate shipment ownership
        Shipment shipment = findOwnedShipment(request.getShipmentId(), driver.getId());

        if (Shipment.STATUS_DELIVERED.equals(shipment.getStatus())
                || Shipment.STATUS_RETURNED.equals(shipment.getStatus())) {
            throw new BadRequestException("Cannot send a report for a completed shipment");
        }

        // Persist as a special tracking checkpoint so the history captures the event
        ShipmentTracking tracking = new ShipmentTracking();
        tracking.setShipmentId(request.getShipmentId());
        tracking.setStatus("REPORT_" + request.getReportType().toUpperCase());
        tracking.setGpsLat(request.getGpsLat() != null ? request.getGpsLat() : 0.0);
        tracking.setGpsLng(request.getGpsLng() != null ? request.getGpsLng() : 0.0);
        tracking.setNotes("[" + request.getReportType() + "] " + request.getDescription());
        TrackingResponse saved = TrackingResponse.from(trackingRepository.save(tracking));

        // Notify Shipping Manager — look up via Order→no direct SM link, use a broadcast type
        // notification so any SHIPPING_MGR user sees it in their in-app inbox
        User driverUser = driver.getUserId() != null
                ? userRepository.findById(driver.getUserId()).orElse(null) : null;
        String driverName = driverUser != null ? driverUser.getFullName() : "Tài xế #" + driver.getId();

        orderRepository.findById(shipment.getOrderId()).ifPresent(order -> {
            // Notify the retailer as well so they are aware of delays/incidents
            if (order.getRetailerId() != null) {
                notificationService.sendNotification(order.getRetailerId(), "WARNING",
                        "Báo cáo từ tài xế: " + request.getReportType(),
                        "Tài xế " + driverName + " đã gửi báo cáo cho đơn hàng #" + order.getId()
                                + ". " + request.getDescription(),
                        false);
            }
        });

        return saved;
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    /** BR5: Driver may only access their own shipments. */
    private Shipment findOwnedShipment(Long shipmentId, Long driverId) {
        Shipment s = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + shipmentId));
        if (!driverId.equals(s.getDriverId())) {
            throw new ForbiddenException("Shipment does not belong to the current driver");
        }
        return s;
    }

    private ShipmentResponse buildResponse(Shipment s, Driver driver) {
        Order order = s.getOrderId() != null
                ? orderRepository.findById(s.getOrderId()).orElse(null) : null;
        User driverUser = driver.getUserId() != null
                ? userRepository.findById(driver.getUserId()).orElse(null) : null;
        Vehicle vehicle = s.getVehicleId() != null
                ? vehicleRepository.findById(s.getVehicleId()).orElse(null) : null;
        return ShipmentResponse.from(s, order, driver, driverUser, vehicle);
    }

    private ShipmentDetailResponse buildDetailResponse(Shipment s, Driver driver) {
        Order order = s.getOrderId() != null
                ? orderRepository.findById(s.getOrderId()).orElse(null) : null;
        User driverUser = driver.getUserId() != null
                ? userRepository.findById(driver.getUserId()).orElse(null) : null;
        Vehicle vehicle = s.getVehicleId() != null
                ? vehicleRepository.findById(s.getVehicleId()).orElse(null) : null;
        List<TrackingResponse> tracking = trackingRepository
                .findByShipmentIdOrderByTimestampDesc(s.getId()).stream()
                .map(TrackingResponse::from)
                .toList();
        return ShipmentDetailResponse.fromDetail(s, order, driver, driverUser, vehicle, tracking);
    }

    /** Resolves the current JWT user to a Driver profile. */
    private Driver requireDriverProfile() {
        User actor = CurrentUser.get();
        ActorAuthorizer.requireRoles(actor, SHIP_DRIVER_ROLES);
        return driverRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No driver profile found for user: " + actor.getId()));
    }

    /**
     * Haversine formula — great-circle distance between two GPS points in kilometres.
     * Used for BR4 GPS plausibility check.
     */
    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s.trim().toUpperCase();
    }
}
