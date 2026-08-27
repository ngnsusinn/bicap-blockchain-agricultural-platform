package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.dto.DriverResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.OrderResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentCreateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentTrackingResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.VehicleResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.*;
import vn.courses.ut.edu.javaprogramming.bicap.exception.*;
import vn.courses.ut.edu.javaprogramming.bicap.repository.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic cho module Shipping.
 * Implements BICAP-54, BICAP-55, BICAP-56, BICAP-57.
 */
@Service
@Transactional
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentTrackingRepository trackingRepository;
    private final OrderRepository orderRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public ShipmentService(ShipmentRepository shipmentRepository,
                           ShipmentTrackingRepository trackingRepository,
                           OrderRepository orderRepository,
                           DriverRepository driverRepository,
                           VehicleRepository vehicleRepository,
                           UserRepository userRepository,
                           ProductRepository productRepository) {
        this.shipmentRepository = shipmentRepository;
        this.trackingRepository = trackingRepository;
        this.orderRepository = orderRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    /**
     * BICAP-54: Lấy danh sách đơn hàng đã thanh toán cọc, chờ tạo lô vận chuyển.
     * Điều kiện: order.status = DEPOSIT_PAID và chưa có shipment nào cho order đó.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getCompletedOrders() {
        return orderRepository.findAll().stream()
                .filter(o -> Order.STATUS_DEPOSIT_PAID.equals(o.getStatus()))
                .filter(o -> shipmentRepository.findByOrderId(o.getId()).isEmpty())
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }

    /**
     * BICAP-55: Tạo lô vận chuyển mới.
     * BR1: Order phải có trạng thái DEPOSIT_PAID.
     * BR2: Driver phải ở trạng thái IDLE.
     * BR3: Vehicle phải ở trạng thái AVAILABLE.
     * BR4: Order chưa có shipment.
     */
    public ShipmentResponse createShipment(ShipmentCreateRequest request) {
        // Validate order
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng id: " + request.getOrderId()));
        if (!Order.STATUS_DEPOSIT_PAID.equals(order.getStatus())) {
            throw new BadRequestException("Chỉ có thể tạo lô vận chuyển cho đơn hàng đã thanh toán cọc (DEPOSIT_PAID). Trạng thái hiện tại: " + order.getStatus());
        }
        if (shipmentRepository.findByOrderId(order.getId()).isPresent()) {
            throw new ConflictException("Đơn hàng này đã có lô vận chuyển.");
        }

        // BR2: Validate driver
        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài xế id: " + request.getDriverId()));
        if (!Driver.STATUS_IDLE.equals(driver.getStatus())) {
            throw new BadRequestException("Tài xế phải ở trạng thái IDLE. Trạng thái hiện tại: " + driver.getStatus());
        }

        // BR3: Validate vehicle
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phương tiện id: " + request.getVehicleId()));
        if (!Vehicle.STATUS_AVAILABLE.equals(vehicle.getStatus())) {
            throw new BadRequestException("Phương tiện phải ở trạng thái AVAILABLE. Trạng thái hiện tại: " + vehicle.getStatus());
        }

        // Create shipment
        Shipment shipment = new Shipment();
        shipment.setOrderId(order.getId());
        shipment.setDriverId(driver.getId());
        shipment.setVehicleId(vehicle.getId());
        shipment.setStatus(Shipment.STATUS_PICKING_UP);
        if (request.getRouteSummary() != null) {
            shipment.setRouteSummary(request.getRouteSummary().trim());
        }
        shipment = shipmentRepository.save(shipment);

        // Update driver and vehicle status
        driver.setStatus(Driver.STATUS_ON_TRIP);
        driverRepository.save(driver);
        vehicle.setStatus(Vehicle.STATUS_IN_USE);
        vehicleRepository.save(vehicle);

        return toShipmentResponse(shipment, false);
    }

    /**
     * BICAP-56: Hủy lô vận chuyển.
     * BR1: Chỉ có thể hủy khi status = PICKING_UP.
     * Sau khi hủy: driver trở về IDLE, vehicle trở về AVAILABLE.
     */
    public void cancelShipment(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lô vận chuyển id: " + shipmentId));
        if (!Shipment.STATUS_PICKING_UP.equals(shipment.getStatus())) {
            throw new BadRequestException("Chỉ có thể hủy lô vận chuyển khi đang ở trạng thái PICKING_UP. Trạng thái hiện tại: " + shipment.getStatus());
        }

        shipment.setStatus(Shipment.STATUS_RETURNED);
        shipmentRepository.save(shipment);

        // Release driver and vehicle
        if (shipment.getDriverId() != null) {
            driverRepository.findById(shipment.getDriverId()).ifPresent(driver -> {
                driver.setStatus(Driver.STATUS_IDLE);
                driverRepository.save(driver);
            });
        }
        if (shipment.getVehicleId() != null) {
            vehicleRepository.findById(shipment.getVehicleId()).ifPresent(vehicle -> {
                vehicle.setStatus(Vehicle.STATUS_AVAILABLE);
                vehicleRepository.save(vehicle);
            });
        }
    }

    /**
     * BICAP-57: Lấy chi tiết lô vận chuyển kèm lịch sử tracking.
     */
    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentDetail(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lô vận chuyển id: " + shipmentId));
        return toShipmentResponse(shipment, true);
    }

    /**
     * Lấy danh sách tất cả các lô vận chuyển (phân trang).
     */
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getAllShipments() {
        return shipmentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(s -> toShipmentResponse(s, false))
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách phương tiện (để populate form tạo lô).
     */
    @Transactional(readOnly = true)
    public List<VehicleResponse> getVehicles() {
        return vehicleRepository.findAll().stream()
                .map(this::toVehicleResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách tài xế (để populate form tạo lô).
     */
    @Transactional(readOnly = true)
    public List<DriverResponse> getDrivers() {
        return driverRepository.findAll().stream()
                .map(this::toDriverResponse)
                .collect(Collectors.toList());
    }

    // ─── Mapping helpers ────────────────────────────────────────────────────

    private OrderResponse toOrderResponse(Order order) {
        OrderResponse resp = new OrderResponse();
        resp.setId(order.getId());
        resp.setStatus(order.getStatus());
        resp.setCreatedAt(order.getCreatedAt());
        resp.setDeliveryAddr(order.getDeliveryAddr());
        resp.setQuantity(order.getQuantity());
        resp.setPrice(order.getPrice());
        resp.setDepositRate(order.getDepositRate());
        resp.setDepositAmount(order.getDepositAmount());
        resp.setDepositCode(order.getDepositCode());

        BigDecimal total = order.getPrice() != null && order.getQuantity() != null
                ? order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()))
                : null;
        resp.setTotalAmount(total);

        // Product info
        productRepository.findById(order.getProductId()).ifPresent(product -> {
            resp.setProductId(product.getId());
            resp.setProductName(product.getName());
            resp.setProductPrice(product.getPrice());
            resp.setProductQuantity(product.getQuantity());
        });

        // Retailer info
        userRepository.findById(order.getRetailerId()).ifPresent(retailer -> {
            resp.setRetailerId(retailer.getId());
            resp.setRetailerName(retailer.getFullName());
            resp.setRetailerEmail(retailer.getEmail());
            resp.setRetailerPhone(retailer.getPhone());
        });

        return resp;
    }

    private ShipmentResponse toShipmentResponse(Shipment shipment, boolean includeTracking) {
        ShipmentResponse resp = new ShipmentResponse();
        resp.setId(shipment.getId());
        resp.setStatus(shipment.getStatus());
        resp.setPickupTime(shipment.getPickupTime());
        resp.setDeliveryTime(shipment.getDeliveryTime());
        resp.setRouteSummary(shipment.getRouteSummary());
        resp.setCreatedAt(shipment.getCreatedAt());
        resp.setOrderId(shipment.getOrderId());

        // Order + product + retailer info
        orderRepository.findById(shipment.getOrderId()).ifPresent(order -> {
            resp.setOrderStatus(order.getStatus());
            resp.setDeliveryAddr(order.getDeliveryAddr());
            resp.setQuantity(order.getQuantity());
            BigDecimal total = order.getPrice() != null && order.getQuantity() != null
                    ? order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()))
                    : null;
            resp.setTotalAmount(total);

            productRepository.findById(order.getProductId()).ifPresent(product ->
                    resp.setProductName(product.getName()));

            userRepository.findById(order.getRetailerId()).ifPresent(retailer -> {
                resp.setRetailerName(retailer.getFullName());
                resp.setRetailerEmail(retailer.getEmail());
            });
        });

        // Driver info
        if (shipment.getDriverId() != null) {
            resp.setDriverId(shipment.getDriverId());
            driverRepository.findById(shipment.getDriverId()).ifPresent(driver -> {
                resp.setDriverLicenseNumber(driver.getLicenseNumber());
                resp.setDriverCitizenId(driver.getCitizenId());
                userRepository.findById(driver.getUserId()).ifPresent(user -> {
                    resp.setDriverName(user.getFullName());
                    resp.setDriverPhone(user.getPhone());
                });
            });
        }

        // Vehicle info
        if (shipment.getVehicleId() != null) {
            resp.setVehicleId(shipment.getVehicleId());
            vehicleRepository.findById(shipment.getVehicleId()).ifPresent(vehicle -> {
                resp.setVehicleLicensePlate(vehicle.getLicensePlate());
                resp.setVehicleType(vehicle.getType());
                resp.setVehicleCapacity(vehicle.getCapacity());
            });
        }

        // Tracking history (BICAP-57)
        if (includeTracking) {
            List<ShipmentTrackingResponse> tracking = trackingRepository
                    .findByShipmentIdOrderByTimestampAsc(shipment.getId())
                    .stream()
                    .map(this::toTrackingResponse)
                    .collect(Collectors.toList());
            resp.setTrackingHistory(tracking);
        }

        return resp;
    }

    private ShipmentTrackingResponse toTrackingResponse(ShipmentTracking t) {
        ShipmentTrackingResponse resp = new ShipmentTrackingResponse();
        resp.setId(t.getId());
        resp.setShipmentId(t.getShipmentId());
        resp.setStatus(t.getStatus());
        resp.setGpsLat(t.getGpsLat());
        resp.setGpsLng(t.getGpsLng());
        resp.setImages(t.getImages());
        resp.setNotes(t.getNotes());
        resp.setTimestamp(t.getTimestamp());
        return resp;
    }

    private VehicleResponse toVehicleResponse(Vehicle v) {
        VehicleResponse resp = new VehicleResponse();
        resp.setId(v.getId());
        resp.setLicensePlate(v.getLicensePlate());
        resp.setType(v.getType());
        resp.setCapacity(v.getCapacity());
        resp.setStatus(v.getStatus());
        resp.setCreatedAt(v.getCreatedAt());
        return resp;
    }

    private DriverResponse toDriverResponse(Driver d) {
        DriverResponse resp = new DriverResponse();
        resp.setId(d.getId());
        resp.setUserId(d.getUserId());
        resp.setCitizenId(d.getCitizenId());
        resp.setLicenseNumber(d.getLicenseNumber());
        resp.setVehicleId(d.getVehicleId());
        resp.setStatus(d.getStatus());
        resp.setCreatedAt(d.getCreatedAt());
        userRepository.findById(d.getUserId()).ifPresent(user -> {
            resp.setFullName(user.getFullName());
            resp.setPhone(user.getPhone());
        });
        if (d.getVehicleId() != null) {
            vehicleRepository.findById(d.getVehicleId()).ifPresent(v ->
                    resp.setVehicleLicensePlate(v.getLicensePlate()));
        }
        return resp;
    }
}
