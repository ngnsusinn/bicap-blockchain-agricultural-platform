package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentDetailResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.TrackingResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Driver;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Shipment;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Vehicle;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
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
 * Retailer read-only views of shipment process for orders they placed
 * (BICAP-49 / SRS-RT-014).
 *
 * <p>A shipment belongs to a retailer when its linked order's {@code retailerId}
 * matches the authenticated user's id. The service is strictly read-only — no
 * state mutations are allowed here.
 */
@Service
@Transactional(readOnly = true)
@SuppressWarnings("null")
public class RetailerShipmentService {

    private static final Set<String> RETAILER_ROLES = Set.of("RETAILER");

    private final ShipmentRepository shipmentRepository;
    private final ShipmentTrackingRepository trackingRepository;
    private final OrderRepository orderRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public RetailerShipmentService(ShipmentRepository shipmentRepository,
                                   ShipmentTrackingRepository trackingRepository,
                                   OrderRepository orderRepository,
                                   DriverRepository driverRepository,
                                   VehicleRepository vehicleRepository,
                                   UserRepository userRepository) {
        this.shipmentRepository = shipmentRepository;
        this.trackingRepository = trackingRepository;
        this.orderRepository = orderRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    /**
     * Danh sách lô vận chuyển cho các đơn hàng của retailer hiện tại (BICAP-49).
     *
     * @param status optional status filter (null = all)
     */
    public List<ShipmentResponse> getMyShipments(String status) {
        User actor = requireRetailer();
        String normalized = (status == null || status.isBlank()) ? null : status.trim().toUpperCase();
        return shipmentRepository.findByRetailerId(actor.getId(), normalized).stream()
                .map(this::buildResponse)
                .toList();
    }

    /**
     * Chi tiết một lô vận chuyển kèm lịch sử GPS — chỉ khi đơn thuộc retailer (BICAP-49).
     *
     * @throws ResourceNotFoundException nếu không tìm thấy shipment
     * @throws ForbiddenException nếu shipment không thuộc đơn của retailer hiện tại
     */
    public ShipmentDetailResponse getMyShipmentDetail(Long shipmentId) {
        User actor = requireRetailer();
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + shipmentId));

        // Verify ownership: the order linked to this shipment must belong to this retailer
        if (shipment.getOrderId() != null) {
            Order order = orderRepository.findById(shipment.getOrderId()).orElse(null);
            if (order == null || !actor.getId().equals(order.getRetailerId())) {
                throw new ForbiddenException("Shipment does not belong to any of your orders");
            }
        } else {
            throw new ForbiddenException("Shipment has no linked order");
        }

        return buildDetailResponse(shipment);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User requireRetailer() {
        User actor = CurrentUser.get();
        ActorAuthorizer.requireRoles(actor, RETAILER_ROLES);
        return actor;
    }

    private ShipmentResponse buildResponse(Shipment s) {
        Order order = orderFor(s);
        Driver driver = driverFor(s);
        User driverUser = userFor(driver);
        Vehicle vehicle = vehicleFor(s);
        return ShipmentResponse.from(s, order, driver, driverUser, vehicle);
    }

    private ShipmentDetailResponse buildDetailResponse(Shipment s) {
        Order order = orderFor(s);
        Driver driver = driverFor(s);
        User driverUser = userFor(driver);
        Vehicle vehicle = vehicleFor(s);
        List<TrackingResponse> tracking = trackingRepository
                .findByShipmentIdOrderByTimestampDesc(s.getId()).stream()
                .map(TrackingResponse::from)
                .toList();
        return ShipmentDetailResponse.fromDetail(s, order, driver, driverUser, vehicle, tracking);
    }

    private Order orderFor(Shipment s) {
        return s.getOrderId() != null ? orderRepository.findById(s.getOrderId()).orElse(null) : null;
    }

    private Driver driverFor(Shipment s) {
        return s.getDriverId() != null ? driverRepository.findById(s.getDriverId()).orElse(null) : null;
    }

    private User userFor(Driver driver) {
        return (driver != null && driver.getUserId() != null)
                ? userRepository.findById(driver.getUserId()).orElse(null) : null;
    }

    private Vehicle vehicleFor(Shipment s) {
        return s.getVehicleId() != null ? vehicleRepository.findById(s.getVehicleId()).orElse(null) : null;
    }
}
