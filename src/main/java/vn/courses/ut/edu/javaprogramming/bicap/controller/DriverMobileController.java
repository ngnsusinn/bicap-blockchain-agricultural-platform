package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.dto.*;
import vn.courses.ut.edu.javaprogramming.bicap.service.DriverShipmentService;

import java.util.List;

/**
 * Driver mobile app endpoints (BICAP-76 / detail-design §2.7).
 *
 * <p>All routes require SHIP_DRIVER role — enforced inside the service.
 * The driver identity is resolved from the JWT principal, not from a request header.
 */
@RestController
@RequestMapping("/api/driver")
public class DriverMobileController {

    private final DriverShipmentService driverShipmentService;

    public DriverMobileController(DriverShipmentService driverShipmentService) {
        this.driverShipmentService = driverShipmentService;
    }

    /** Driver's own shipments, optionally filtered by status. */
    @GetMapping("/shipments")
    public ResponseEntity<List<ShipmentResponse>> getMyShipments(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(driverShipmentService.getMyShipments(status));
    }

    /** Full detail of one shipment (with tracking history) — must belong to the driver. */
    @GetMapping("/shipments/{id}")
    public ResponseEntity<ShipmentDetailResponse> getShipmentDetail(@PathVariable Long id) {
        return ResponseEntity.ok(driverShipmentService.getShipmentDetail(id));
    }

    /** Adds a GPS tracking checkpoint. Allowed in both PICKING_UP and IN_TRANSIT states. */
    @PostMapping("/shipments/{id}/tracking")
    public ResponseEntity<TrackingResponse> addTracking(
            @PathVariable Long id,
            @Valid @RequestBody TrackingAddRequest request) {
        return ResponseEntity.ok(driverShipmentService.addTracking(id, request));
    }

    /** Confirms goods pickup at the farm — transitions shipment PICKING_UP → IN_TRANSIT. */
    @PostMapping("/shipments/{id}/pickup")
    public ResponseEntity<ShipmentDetailResponse> confirmPickup(
            @PathVariable Long id,
            @Valid @RequestBody PickupConfirmRequest request) {
        return ResponseEntity.ok(driverShipmentService.confirmPickup(id, request));
    }

    /** Confirms successful delivery — transitions shipment IN_TRANSIT → DELIVERED, order → DELIVERED. */
    @PostMapping("/shipments/{id}/deliver")
    public ResponseEntity<ShipmentDetailResponse> confirmDelivery(
            @PathVariable Long id,
            @Valid @RequestBody DeliveryConfirmRequest request) {
        return ResponseEntity.ok(driverShipmentService.confirmDelivery(id, request));
    }

    /**
     * Driver sends an incident/delay/damage report (BICAP-76 / detail-design §2.7).
     * Report is stored as a special tracking checkpoint and triggers a notification.
     */
    @PostMapping("/reports")
    public ResponseEntity<TrackingResponse> sendReport(
            @Valid @RequestBody DriverReportRequest request) {
        return ResponseEntity.ok(driverShipmentService.sendReport(request));
    }
}
