package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmShipmentSummaryResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentDetailResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.TrackingResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Driver;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Shipment;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Vehicle;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.DriverRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.OrderRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ShipmentRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ShipmentTrackingRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.VehicleRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Farm Manager read-only views of the shipping process for goods that originated
 * from their own farm (BICAP-22 / SRS-FM-016 and BICAP-23 / SRS-FM-017).
 *
 * <p>A shipment is "owned" by a farm when its order's product traces back to a
 * farming season belonging to that farm (shipment → order → product → season → farm).
 */
@Service
@Transactional(readOnly = true)
@SuppressWarnings("null")
public class FarmShipmentService {

    private static final Set<String> FARM_MANAGER_ROLES = Set.of("FARM_MANAGER");

    private final FarmRepository farmRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentTrackingRepository trackingRepository;
    private final OrderRepository orderRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public FarmShipmentService(FarmRepository farmRepository,
                               ShipmentRepository shipmentRepository,
                               ShipmentTrackingRepository trackingRepository,
                               OrderRepository orderRepository,
                               DriverRepository driverRepository,
                               VehicleRepository vehicleRepository,
                               UserRepository userRepository) {
        this.farmRepository = farmRepository;
        this.shipmentRepository = shipmentRepository;
        this.trackingRepository = trackingRepository;
        this.orderRepository = orderRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    /** Danh sách lô vận chuyển liên quan đến nông trại (BICAP-22). */
    public List<ShipmentResponse> getFarmShipments(Long farmId, String status) {
        Farm farm = requireOwnedFarm(farmId);
        String normalized = (status == null || status.isBlank()) ? null : status.trim().toUpperCase();
        return shipmentRepository.findByFarmId(farm.getId(), normalized).stream()
                .map(this::buildResponse)
                .toList();
    }

    /** Chi tiết một lô vận chuyển, kèm lịch sử GPS — chỉ khi lô thuộc nông trại (BICAP-22). */
    public ShipmentDetailResponse getFarmShipmentDetail(Long farmId, Long shipmentId) {
        Farm farm = requireOwnedFarm(farmId);
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + shipmentId));
        if (!belongsToFarm(shipment, farm.getId())) {
            throw new ForbiddenException("Shipment does not belong to this farm");
        }
        return buildDetailResponse(shipment);
    }

    /** Báo cáo tổng hợp quy trình vận chuyển của nông trại (BICAP-23 / SRS-FM-017). */
    public FarmShipmentSummaryResponse getFarmShipmentSummary(Long farmId) {
        Farm farm = requireOwnedFarm(farmId);
        List<Shipment> shipments = shipmentRepository.findByFarmId(farm.getId(), null);

        Map<String, Long> byStatus = new HashMap<>();
        long onTime = 0;
        long late = 0;
        for (Shipment s : shipments) {
            byStatus.merge(s.getStatus(), 1L, Long::sum);
            if (Shipment.STATUS_DELIVERED.equals(s.getStatus()) && s.getDeliveryTime() != null) {
                Order order = s.getOrderId() != null
                        ? orderRepository.findById(s.getOrderId()).orElse(null) : null;
                if (order != null && order.getDesiredDeliveryDate() != null) {
                    if (s.getDeliveryTime().toLocalDate().isAfter(order.getDesiredDeliveryDate())) {
                        late++;
                    } else {
                        onTime++;
                    }
                } else {
                    onTime++;
                }
            }
        }
        return FarmShipmentSummaryResponse.of(farm.getId(), byStatus, onTime, late);
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private boolean belongsToFarm(Shipment shipment, Long farmId) {
        return shipmentRepository.findByFarmId(farmId, null).stream()
                .anyMatch(s -> s.getId().equals(shipment.getId()));
    }

    private Farm requireOwnedFarm(Long farmId) {
        User actor = CurrentUser.get();
        ActorAuthorizer.requireRoles(actor, FARM_MANAGER_ROLES);
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found: " + farmId));
        if (!farm.getUserId().equals(actor.getId())) {
            throw new ForbiddenException("Farm does not belong to current user");
        }
        return farm;
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
