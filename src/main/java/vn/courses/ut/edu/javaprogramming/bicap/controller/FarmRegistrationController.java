package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmRegistrationRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.service.FarmRegistrationService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Farm Manager self-registration of a farm profile (H-7 / BICAP-7).
 * The registered farm enters the admin approval queue as PENDING (BICAP-3).
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
