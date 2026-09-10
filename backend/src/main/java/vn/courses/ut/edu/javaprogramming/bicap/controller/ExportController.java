package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ExportCreateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ExportResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Export;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingSeasonRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.ExportService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/seasons/{seasonId}/exports")
public class ExportController {

    private final ExportService exportService;
    private final FarmingSeasonRepository seasonRepository;
    private final FarmRepository farmRepository;

    public ExportController(ExportService exportService, FarmingSeasonRepository seasonRepository, FarmRepository farmRepository) {
        this.exportService = exportService;
        this.seasonRepository = seasonRepository;
        this.farmRepository = farmRepository;
    }

    @PostMapping
    public ResponseEntity<ExportResponse> createExport(
            @PathVariable Long seasonId,
            @Valid @RequestBody ExportCreateRequest request) {
        User currentUser = CurrentUser.get();
        FarmingSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Season not found"));
        
        Farm farm = farmRepository.findById(season.getFarmId())
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
        
        if (!farm.getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("Not authorized to export for this season");
        }

        if (!"HARVESTED".equals(season.getStatus())) {
            throw new BadRequestException("Season status must be HARVESTED for export");
        }

        Export export = new Export();
        export.setSeasonId(seasonId);
        export.setExportDate(request.getExportDate());
        export.setQuantity(request.getQuantity());
        export.setDestination(request.getDestination());
        
        Export createdExport = exportService.createExport(export);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(createdExport));
    }

    @GetMapping
    public ResponseEntity<List<ExportResponse>> getExports(@PathVariable Long seasonId) {
        // Validation could be added here if needed, but exports might be readable
        List<Export> exports = exportService.getExportsBySeason(seasonId);
        List<ExportResponse> response = exports.stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{exportId}")
    public ResponseEntity<ExportResponse> getExport(
            @PathVariable Long seasonId,
            @PathVariable Long exportId) {
        Export export = exportService.getExport(exportId)
                .orElseThrow(() -> new ResourceNotFoundException("Export not found: " + exportId));
        if (!export.getSeasonId().equals(seasonId)) {
            throw new BadRequestException("Export does not belong to this season");
        }
        return ResponseEntity.ok(toResponse(export));
    }

    private ExportResponse toResponse(Export e) {
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
