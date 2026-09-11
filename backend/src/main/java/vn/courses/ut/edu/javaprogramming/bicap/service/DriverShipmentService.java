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
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Product;
import vn.courses.ut.edu.javaprogramming.bicap.entity.SeasonExport;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Shipment;
import vn.courses.ut.edu.javaprogramming.bicap.entity.ShipmentTracking;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Vehicle;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.DriverRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingSeasonRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.OrderRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ProductRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.SeasonExportRepository;
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
    private final ProductRepository         productRepository;
    private final FarmingSeasonRepository   seasonRepository;
    private final FarmRepository            farmRepository;
    private final SeasonExportRepository    seasonExportRepository;
    private final NotificationService       notificationService;

    public DriverShipmentService(ShipmentRepository shipmentRepository,
                                 ShipmentTrackingRepository trackingRepository,
                                 OrderRepository orderRepository,
                                 DriverRepository driverRepository,
                                 VehicleRepository vehicleRepository,
                                 UserRepository userRepository,
                                 ProductRepository productRepository,
                                 FarmingSeasonRepository seasonRepository,
                                 FarmRepository farmRepository,
                                 SeasonExportRepository seasonExportRepository,
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
        this.seasonExportRepository = seasonExportRepository;
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
     *
     * <p>F11: the driver must scan the product's traceability QR — the scanned hash is
     * matched against the export attached to the ordered product before the shipment moves.
     */
    public ShipmentDetailResponse confirmPickup(Long shipmentId, PickupConfirmRequest request) {
        Driver driver = requireDriverProfile();
        Shipment shipment = findOwnedShipment(shipmentId, driver.getId());

        if (!Shipment.STATUS_PICKING_UP.equals(shipment.getStatus())) {
            throw new BadRequestException(
                    "Shipment must be in PICKING_UP state to confirm pickup (current: " + shipment.getStatus() + ")");
        }

        verifyPickupTraceHash(shipment, request.getTraceHash());

        // Persist tracking checkpoint
        ShipmentTracking tracking = new ShipmentTracking();
        tracking.setShipmentId(shipmentId);
        tracking.setStatus("PICKUP_CONFIRMED");
        tracking.setGpsLat(request.getGpsLat());
        tracking.setGpsLng(request.getGpsLng());
        tracking.setImages(ImagesJson.toJson(request.getImages()));
        tracking.setNotes(request.getNotes() == null ? null
                : request.getNotes() + (request.getTraceHash() != null
                        ? " [QR:" + request.getTraceHash().trim() + "]" : ""));
        trackingRepository.save(tracking);

        shipment.setStatus(Shipment.STATUS_IN_TRANSIT);
        shipment.setPickupTime(LocalDateTime.now());
        shipmentRepository.save(shipment);

        // BICAP-50: notify retailer that shipment is on the way
        orderRepository.findById(shipment.getOrderId()).ifPresent(order -> {
            if (order.getRetailerId() != null) {
                notificationService.sendNotification(order.getRetailerId(), "INFO",
                        "Đơn hàng đang được vận chuyển",
                        "Tài xế đã lấy hàng cho đơn #" + order.getId()
                                + ". Đơn hàng đang trên đường giao tới bạn.",
                        false);
            }
        });

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

        // Transition order to DELIVERED and notify retailer + farm manager (BICAP-61)
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

            // Notify Farm Manager that their goods were delivered (BICAP-61)
            if (order.getProductId() != null) {
                productRepository.findById(order.getProductId()).ifPresent(product -> {
                    if (product.getSeasonId() != null) {
                        seasonRepository.findById(product.getSeasonId()).ifPresent(season -> {
                            if (season.getFarmId() != null) {
                                farmRepository.findById(season.getFarmId()).ifPresent(farm -> {
                                    if (farm.getUserId() != null) {
                                        notificationService.sendNotification(farm.getUserId(), "SUCCESS",
                                                "Hàng của bạn đã được giao thành công",
                                                "Sản phẩm từ đơn hàng #" + order.getId()
                                                        + " đã được giao đến nhà bán lẻ.",
                                                false);
                                    }
                                });
                            }
                        });
                    }
                });
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

        // F10 fix: the driver report must reach the Shipping Manager. The previous code
        // only notified the retailer (despite the comment claiming otherwise), so an
        // incident/delay report never showed up in the Shipping Manager's inbox.
        User driverUser = driver.getUserId() != null
                ? userRepository.findById(driver.getUserId()).orElse(null) : null;
        String driverName = driverUser != null ? driverUser.getFullName() : "Tài xế #" + driver.getId();

        final Long orderId = shipment.getOrderId();
        userRepository.findDistinctByRoles_NameIn(Set.of("SHIPPING_MGR")).stream()
                .filter(manager -> manager.getStatus() == UserStatus.ACTIVE)
                .forEach(manager -> notificationService.sendNotification(manager.getId(), "WARNING",
                        "Báo cáo từ tài xế: " + request.getReportType(),
                        "Tài xế " + driverName + " báo cáo cho lô #" + shipment.getId()
                                + " (đơn #" + orderId + "). " + request.getDescription(),
                        false));

        orderRepository.findById(orderId).ifPresent(order -> {
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

    /**
     * F11: verifies the QR scanned at the farm against the trace hash of the export that
     * backs the ordered product. A product whose export has no trace hash yet cannot be
     * verified, so a scan is rejected as well rather than silently accepted.
     */
    private void verifyPickupTraceHash(Shipment shipment, String scannedTraceHash) {
        Order order = shipment.getOrderId() != null
                ? orderRepository.findById(shipment.getOrderId()).orElse(null) : null;
        Product product = (order != null && order.getProductId() != null)
                ? productRepository.findById(order.getProductId()).orElse(null) : null;
        String expected = (product != null && product.getExportId() != null)
                ? seasonExportRepository.findById(product.getExportId())
                        .map(SeasonExport::getTraceHash).orElse(null)
                : null;

        if (expected == null || expected.isBlank()) {
            throw new BadRequestException(
                    "Lô hàng này chưa có mã truy xuất blockchain để đối chiếu — không thể xác nhận lấy hàng");
        }
        if (scannedTraceHash == null || scannedTraceHash.isBlank()) {
            throw new BadRequestException("Vui lòng quét mã QR truy xuất của lô hàng trước khi xác nhận lấy hàng");
        }
        if (!expected.equalsIgnoreCase(scannedTraceHash.trim())) {
            throw new BadRequestException("Mã QR không khớp với lô hàng được phân công");
        }
    }

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
