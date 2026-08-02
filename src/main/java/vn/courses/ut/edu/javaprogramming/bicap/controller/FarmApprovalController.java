package vn.courses.ut.edu.javaprogramming.bicap.controller;

import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmApprovalRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmDetailResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmNotesUpdateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmStatusUpdateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmStatus;
import vn.courses.ut.edu.javaprogramming.bicap.service.FarmApprovalService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin endpoints for farm registration approval (BICAP-3 / SRS-ADM-002).
 */
@RestController
@RequestMapping("/api/admin/farms")
public class FarmApprovalController {

    private final FarmApprovalService farmApprovalService;

    public FarmApprovalController(FarmApprovalService farmApprovalService) {
        this.farmApprovalService = farmApprovalService;
    }

    @GetMapping
    public ResponseEntity<Page<FarmResponse>> getFarms(
            @RequestHeader("X-Actor-Email") String actorEmail,
            @RequestParam(required = false) FarmStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<FarmResponse> farms = farmApprovalService.getFarms(status, search, pageable, actorEmail);
        return ResponseEntity.ok(farms);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FarmDetailResponse> getFarmDetail(
            @RequestHeader("X-Actor-Email") String actorEmail,
            @PathVariable Long id) {
        FarmDetailResponse farm = farmApprovalService.getFarmDetail(id, actorEmail);
        return ResponseEntity.ok(farm);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStatusCounts(@RequestHeader("X-Actor-Email") String actorEmail) {
        return ResponseEntity.ok(farmApprovalService.getStatusCounts(actorEmail));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<FarmResponse> approveFarm(
            @RequestHeader("X-Actor-Email") String actorEmail,
            @PathVariable Long id) {
        FarmResponse approved = farmApprovalService.approveFarm(id, actorEmail);
        return ResponseEntity.ok(approved);
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<FarmResponse> rejectFarm(
            @RequestHeader("X-Actor-Email") String actorEmail,
            @PathVariable Long id,
            @Valid @RequestBody FarmApprovalRequest request) {
        FarmResponse rejected = farmApprovalService.rejectFarm(id, request, actorEmail);
        return ResponseEntity.ok(rejected);
    }

    // ── Farm management (BICAP-4 / SRS-ADM-003) ──

    @PutMapping("/{id}/status")
    public ResponseEntity<FarmResponse> updateStatus(
            @RequestHeader("X-Actor-Email") String actorEmail,
            @PathVariable Long id,
            @Valid @RequestBody FarmStatusUpdateRequest request) {
        FarmResponse updated = farmApprovalService.updateStatus(id, request, actorEmail);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/notes")
    public ResponseEntity<FarmResponse> updateNotes(
            @RequestHeader("X-Actor-Email") String actorEmail,
            @PathVariable Long id,
            @Valid @RequestBody FarmNotesUpdateRequest request) {
        FarmResponse updated = farmApprovalService.updateNotes(id, request, actorEmail);
        return ResponseEntity.ok(updated);
    }
}
