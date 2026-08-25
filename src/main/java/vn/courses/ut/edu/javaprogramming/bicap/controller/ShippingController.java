package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.dto.*;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.service.DriverService;
import vn.courses.ut.edu.javaprogramming.bicap.service.ShipmentService;
import vn.courses.ut.edu.javaprogramming.bicap.service.VehicleService;

import java.util.List;

/**
 * Shipping Manager endpoints (BICAP-76 / detail-design §2.7).
 *
 * <p>All routes require SHIPPING_MGR role — enforced inside each service method.
 */
@RestController
@RequestMapping("/api/shipping")
public class ShippingController {

    private final ShipmentService shipmentService;
    private final VehicleService  vehicleService;
    private final DriverService   driverService;

    public ShippingController(ShipmentService shipmentService,
                              VehicleService vehicleService,
                              DriverService driverService) {
        this.shipmentService = shipmentService;
        this.vehicleService  = vehicleService;
        this.driverService   = driverService;
    }

    // ── ORDERS (read-only — expose DEPOSIT_PAID orders ready for shipment) ─────

    /** Lists orders in DEPOSIT_PAID state waiting for a shipment to be created. */
    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getCompletedOrders() {
        return ResponseEntity.ok(shipmentService.getCompletedOrders());
    }

    // ── SHIPMENTS ─────────────────────────────────────────────────────────────

    /** Lists all shipments with an optional status filter. */
    @GetMapping("/shipments")
    public ResponseEntity<List<ShipmentResponse>> getShipments(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(shipmentService.getShipments(status));
    }

    /** Full detail of a shipment including GPS tracking history. */
    @GetMapping("/shipments/{id}")
    public ResponseEntity<ShipmentDetailResponse> getShipmentDetail(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.getShipmentDetail(id));
    }

    /** Creates a new shipment from a DEPOSIT_PAID order (BR2/BR3/BR4). */
    @PostMapping("/shipments")
    public ResponseEntity<ShipmentResponse> createShipment(
            @Valid @RequestBody ShipmentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shipmentService.createShipment(request));
    }

    /** Cancels a shipment — only allowed when status = PICKING_UP (BR1). */
    @PutMapping("/shipments/{id}/cancel")
    public ResponseEntity<ShipmentResponse> cancelShipment(
            @PathVariable Long id,
            @RequestBody(required = false) ShipmentCancelRequest request) {
        return ResponseEntity.ok(shipmentService.cancelShipment(id, request));
    }

    // ── VEHICLES ──────────────────────────────────────────────────────────────

    @GetMapping("/vehicles")
    public ResponseEntity<List<VehicleResponse>> getVehicles() {
        return ResponseEntity.ok(vehicleService.getVehicles());
    }

    @PostMapping("/vehicles")
    public ResponseEntity<VehicleResponse> createVehicle(
            @Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vehicleService.createVehicle(request));
    }

    @PutMapping("/vehicles/{id}")
    public ResponseEntity<VehicleResponse> updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(vehicleService.updateVehicle(id, request));
    }

    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }

    // ── DRIVERS ───────────────────────────────────────────────────────────────

    @GetMapping("/drivers")
    public ResponseEntity<List<DriverResponse>> getDrivers() {
        return ResponseEntity.ok(driverService.getDrivers());
    }

    @PostMapping("/drivers")
    public ResponseEntity<DriverResponse> createDriver(
            @Valid @RequestBody DriverCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(driverService.createDriver(request));
    }

    @PutMapping("/drivers/{id}")
    public ResponseEntity<DriverResponse> updateDriver(
            @PathVariable Long id,
            @RequestBody DriverUpdateRequest request) {
        return ResponseEntity.ok(driverService.updateDriver(id, request));
    }

    @DeleteMapping("/drivers/{id}")
    public ResponseEntity<Void> deleteDriver(@PathVariable Long id) {
        driverService.deleteDriver(id);
        return ResponseEntity.noContent().build();
    }

    /** Assigns a vehicle to a driver (BR2: driver IDLE, BR3: vehicle AVAILABLE). */
    @PutMapping("/drivers/{id}/assign")
    public ResponseEntity<DriverResponse> assignVehicle(
            @PathVariable Long id,
            @RequestParam Long vehicleId) {
        return ResponseEntity.ok(driverService.assignVehicle(id, vehicleId));
    }
}
