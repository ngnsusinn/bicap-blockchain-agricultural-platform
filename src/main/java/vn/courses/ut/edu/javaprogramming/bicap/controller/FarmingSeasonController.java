package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.dto.*;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingProcess;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Export;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.service.SeasonService;
import vn.courses.ut.edu.javaprogramming.bicap.service.ProcessService;
import vn.courses.ut.edu.javaprogramming.bicap.service.ExportService;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/farms/{farmId}/seasons")
public class FarmingSeasonController {

    private final SeasonService seasonService;
    private final ProcessService processService;
    private final ExportService exportService;
    private final FarmRepository farmRepository;

    public FarmingSeasonController(SeasonService seasonService, ProcessService processService,
                                   ExportService exportService, FarmRepository farmRepository) {
        this.seasonService = seasonService;
        this.processService = processService;
        this.exportService = exportService;
        this.farmRepository = farmRepository;
    }

    @PostMapping
    public ResponseEntity<SeasonResponse> createSeason(
            @PathVariable Long farmId,
            @Valid @RequestBody SeasonCreateRequest request) {
        User user = CurrentUser.get();
        FarmingSeason season = seasonService.createSeason(farmId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(season));
    }

    @GetMapping
    public ResponseEntity<Page<SeasonResponse>> getSeasons(
            @PathVariable Long farmId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = CurrentUser.get();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SeasonResponse> result = seasonService.getSeasonsByFarm(farmId, status, pageable, user)
                .map(this::toResponse);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{seasonId}")
    public ResponseEntity<SeasonDetailResponse> getSeason(
            @PathVariable Long farmId,
            @PathVariable Long seasonId) {
        User user = CurrentUser.get();
        FarmingSeason season = seasonService.getSeason(farmId, seasonId, user);
        // Get processes and exports for detail view
        List<ProcessResponse> processes = processService.getProcessesBySeason(seasonId, user).stream()
                .map(this::toProcessResponse)
                .collect(Collectors.toList());
        List<ExportResponse> exports = exportService.getExportsBySeason(seasonId).stream()
                .map(this::toExportResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(toDetailResponse(season, processes, exports));
    }

    @PutMapping("/{seasonId}")
    public ResponseEntity<SeasonResponse> updateSeason(
            @PathVariable Long farmId,
            @PathVariable Long seasonId,
            @Valid @RequestBody SeasonUpdateRequest request) {
        User user = CurrentUser.get();
        FarmingSeason season = seasonService.updateSeason(farmId, seasonId, request, user);
        return ResponseEntity.ok(toResponse(season));
    }

    @PatchMapping("/{seasonId}/status")
    public ResponseEntity<SeasonResponse> updateSeasonStatus(
            @PathVariable Long farmId,
            @PathVariable Long seasonId,
            @Valid @RequestBody SeasonStatusUpdateRequest request) {
        User user = CurrentUser.get();
        FarmingSeason season = seasonService.updateSeasonStatus(farmId, seasonId, request, user);
        return ResponseEntity.ok(toResponse(season));
    }

    // Private helper methods for mapping entity -> response
    private SeasonResponse toResponse(FarmingSeason s) {
        return SeasonResponse.builder()
                .id(s.getId())
                .farmId(s.getFarmId())
                .name(s.getName())
                .productType(s.getProductType())
                .variety(s.getVariety())
                .area(s.getArea())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .status(s.getStatus())
                .harvestedQuantity(s.getHarvestedQuantity())
                .harvestUnit(s.getHarvestUnit())
                .txHash(s.getTxHash())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private SeasonDetailResponse toDetailResponse(FarmingSeason s, List<ProcessResponse> processes, List<ExportResponse> exports) {
        SeasonDetailResponse detail = new SeasonDetailResponse();
        detail.setId(s.getId());
        detail.setFarmId(s.getFarmId());
        detail.setName(s.getName());
        detail.setProductType(s.getProductType());
        detail.setVariety(s.getVariety());
        detail.setArea(s.getArea());
        detail.setStartDate(s.getStartDate());
        detail.setEndDate(s.getEndDate());
        detail.setStatus(s.getStatus());
        detail.setTxHash(s.getTxHash());
        detail.setCreatedAt(s.getCreatedAt());
        detail.setProcesses(processes);
        detail.setExports(exports);
        // Populate farm name
        farmRepository.findById(s.getFarmId()).ifPresent(farm -> detail.setFarmName(farm.getName()));
        return detail;
    }

    private ProcessResponse toProcessResponse(FarmingProcess p) {
        return ProcessResponse.builder()
                .id(p.getId())
                .seasonId(p.getSeasonId())
                .processType(p.getProcessType())
                .executionDate(p.getExecutionDate())
                .materials(p.getMaterials())
                .images(p.getImages())
                .notes(p.getNotes())
                .txHash(p.getTxHash())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private ExportResponse toExportResponse(Export e) {
        return ExportResponse.builder()
                .id(e.getId())
                .seasonId(e.getSeasonId())
                .exportDate(e.getExportDate())
                .quantity(e.getQuantity())
                .destination(e.getDestination())
                .txHash(e.getTxHash())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
