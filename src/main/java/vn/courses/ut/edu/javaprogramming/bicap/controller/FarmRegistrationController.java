package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmCertificationResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmRegistrationRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmUpdateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.service.FarmRegistrationService;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Farm Manager self-registration of a farm profile (H-7 / BICAP-7).
 * The registered farm enters the admin approval queue as PENDING (BICAP-3).
 *
 * <p>Also exposes BICAP-9 / SRS-FM-003: updating farm details and managing the
 * farm's business-license / certification documents.
 */
@RestController
@RequestMapping("/api/farms")
public class FarmRegistrationController {

    private final FarmRegistrationService farmRegistrationService;

    public FarmRegistrationController(FarmRegistrationService farmRegistrationService) {
        this.farmRegistrationService = farmRegistrationService;
    }

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

    /** Xem chi tiết một nông trại của mình (BICAP-9). */
    @GetMapping("/{farmId}")
    public ResponseEntity<FarmResponse> getFarm(@PathVariable Long farmId) {
        return ResponseEntity.ok(toResponse(farmRegistrationService.getOwnedFarm(farmId)));
    }

    /** Cập nhật thông tin nông trại (BICAP-9 / SRS-FM-003). */
    @PutMapping("/{farmId}")
    public ResponseEntity<FarmResponse> updateFarm(
            @PathVariable Long farmId,
            @Valid @RequestBody FarmUpdateRequest request) {
        return ResponseEntity.ok(toResponse(farmRegistrationService.updateFarm(farmId, request)));
    }

    /** Tải lên giấy phép kinh doanh / chứng nhận cho nông trại (BICAP-9). */
    @PostMapping(value = "/{farmId}/certifications", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FarmCertificationResponse> addCertification(
            @PathVariable Long farmId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                FarmCertificationResponse.fromEntity(
                        farmRegistrationService.addCertification(farmId, type, expiryDate, file)));
    }

    /** Danh sách chứng nhận / giấy phép của nông trại (BICAP-9). */
    @GetMapping("/{farmId}/certifications")
    public ResponseEntity<List<FarmCertificationResponse>> getCertifications(@PathVariable Long farmId) {
        List<FarmCertificationResponse> certs = farmRegistrationService.getCertifications(farmId).stream()
                .map(FarmCertificationResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(certs);
    }

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
