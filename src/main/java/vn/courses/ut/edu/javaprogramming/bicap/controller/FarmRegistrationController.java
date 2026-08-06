package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.dto.AddCertificationRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmCertificationResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmRegistrationRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmUpdateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.service.FarmRegistrationService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Farm Manager self-registration of a farm profile (H-7 / BICAP-7).
 * The registered farm enters the admin approval queue as PENDING (BICAP-3).
 *
 * Extended in BICAP-9 / SRS-FM-003:
 * - PUT  /{id}                         → updateFarm
 * - GET  /{id}/certifications          → getCertifications
 * - POST /{id}/documents               → addCertification (upload giấy phép/chứng nhận)
 * - DELETE /{id}/certifications/{certId} → deleteCertification
 */
@RestController
@RequestMapping("/api/farms")
public class FarmRegistrationController {

    private final FarmRegistrationService farmRegistrationService;

    public FarmRegistrationController(FarmRegistrationService farmRegistrationService) {
        this.farmRegistrationService = farmRegistrationService;
    }

    // ── BICAP-7 (existing) ──────────────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<FarmResponse> registerFarm(@Valid @RequestBody FarmRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                toResponse(farmRegistrationService.registerFarm(request)));
    }

    /** Farms owned by the authenticated user — the farm portal resolves its farmId here (M-2). */
    @GetMapping("/my")
    public ResponseEntity<List<FarmResponse>> getMyFarms() {
        List<FarmResponse> farms = farmRegistrationService.getMyFarms().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(farms);
    }

    // ── BICAP-9 (new) ───────────────────────────────────────────────────────────

    /**
     * Cập nhật thông tin nông trại (BICAP-9 / SRS-FM-003).
     * Nếu user chưa có farm → tạo mới (lần submit đầu tiên).
     * Nếu đã có farm → cập nhật và chuyển trạng thái về PENDING.
     */
    @PutMapping("/{id}")
    public ResponseEntity<FarmResponse> updateFarm(
            @PathVariable Long id,
            @Valid @RequestBody FarmUpdateRequest request) {
        Farm updated = farmRegistrationService.updateFarm(id, request);
        return ResponseEntity.ok(toResponse(updated));
    }

    /**
     * Tạo hoặc cập nhật thông tin nông trại (BICAP-9 / SRS-FM-003).
     * Alternative endpoint không cần farmId — backend tự phát hiện user đã có farm chưa.
     * Nếu chưa có → tạo mới. Nếu đã có → cập nhật farm đầu tiên của user.
     */
    @PostMapping("/upsert")
    public ResponseEntity<FarmResponse> createOrUpdateFarm(@Valid @RequestBody FarmUpdateRequest request) {
        Farm result = farmRegistrationService.createOrUpdateFarm(request);
        return ResponseEntity.ok(toResponse(result));
    }

    /**
     * Lấy danh sách chứng nhận/giấy phép kinh doanh của nông trại (BICAP-9).
     */
    @GetMapping("/{id}/certifications")
    public ResponseEntity<List<FarmCertificationResponse>> getCertifications(@PathVariable Long id) {
        List<FarmCertificationResponse> certs = farmRegistrationService.getCertifications(id);
        return ResponseEntity.ok(certs);
    }

    /**
     * Tải lên giấy phép kinh doanh hoặc thêm chứng nhận mới vào nông trại (BICAP-9 / SRS-FM-003).
     * Đặt tên endpoint /documents theo thiết kế API trong detail-design.md section 6.4.
     */
    @PostMapping("/{id}/documents")
    public ResponseEntity<FarmCertificationResponse> addCertification(
            @PathVariable Long id,
            @Valid @RequestBody AddCertificationRequest request) {
        FarmCertificationResponse cert = farmRegistrationService.addCertification(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(cert);
    }

    /**
     * Xóa chứng nhận/giấy phép khỏi nông trại (BICAP-9).
     */
    @DeleteMapping("/{id}/certifications/{certId}")
    public ResponseEntity<Void> deleteCertification(
            @PathVariable Long id,
            @PathVariable Long certId) {
        farmRegistrationService.deleteCertification(id, certId);
        return ResponseEntity.noContent().build();
    }

    // ── Private helpers ─────────────────────────────────────────────────────────

    private FarmResponse toResponse(Farm farm) {
        return FarmResponse.builder()
                .id(farm.getId())
                .name(farm.getName())
                .address(farm.getAddress())
                .area(farm.getArea())
                .gpsLat(farm.getGpsLat())
                .gpsLng(farm.getGpsLng())
                .description(farm.getDescription())
                .productTypes(farm.getProductTypes())
                .status(farm.getStatus())
                .createdAt(farm.getCreatedAt())
                .updatedAt(farm.getUpdatedAt())
                .build();
    }
}
