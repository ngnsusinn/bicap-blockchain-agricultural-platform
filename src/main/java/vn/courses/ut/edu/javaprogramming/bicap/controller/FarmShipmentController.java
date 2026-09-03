package vn.courses.ut.edu.javaprogramming.bicap.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmShipmentSummaryResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentDetailResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ShipmentResponse;
import vn.courses.ut.edu.javaprogramming.bicap.service.FarmShipmentService;

import java.util.List;

/**
 * Farm Manager xem quy trình vận chuyển cho hàng hóa xuất từ nông trại của mình
 * (BICAP-22 / SRS-FM-016 và BICAP-23 / SRS-FM-017).
 *
 * <p>Read-only — quyền sở hữu nông trại được kiểm tra trong service.
 */
@RestController
@RequestMapping("/api/farms/{farmId}/shipments")
public class FarmShipmentController {

    private final FarmShipmentService farmShipmentService;

    public FarmShipmentController(FarmShipmentService farmShipmentService) {
        this.farmShipmentService = farmShipmentService;
    }

    @GetMapping
    public ResponseEntity<List<ShipmentResponse>> getFarmShipments(
            @PathVariable Long farmId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(farmShipmentService.getFarmShipments(farmId, status));
    }

    @GetMapping("/summary")
    public ResponseEntity<FarmShipmentSummaryResponse> getSummary(@PathVariable Long farmId) {
        return ResponseEntity.ok(farmShipmentService.getFarmShipmentSummary(farmId));
    }

    @GetMapping("/{shipmentId}")
    public ResponseEntity<ShipmentDetailResponse> getShipmentDetail(
            @PathVariable Long farmId,
            @PathVariable Long shipmentId) {
        return ResponseEntity.ok(farmShipmentService.getFarmShipmentDetail(farmId, shipmentId));
    }
}
