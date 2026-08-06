package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ProcessRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ProcessResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.SeasonRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.SeasonResponse;
import vn.courses.ut.edu.javaprogramming.bicap.service.FarmingSeasonService;

import java.util.List;

/**
 * Farm Season endpoints for Farm Manager portal (BICAP-12/13/14/15 / SRS-FM-006/007/008/009).
 * Base path: /api/farms/{farmId}/seasons
 *
 * BICAP-12 — GET  /{seasonId}/processes → danh sách quy trình
 * BICAP-13 — GET  /                     → danh sách mùa vụ (list)
 *            GET  /{seasonId}            → chi tiết mùa vụ kèm quy trình
 * BICAP-14 — POST /                     → tạo mùa vụ mới + Blockchain
 * BICAP-15 — POST /{seasonId}/processes → thêm quy trình + Blockchain
 */
@RestController
@RequestMapping("/api/farms/{farmId}/seasons")
public class FarmingSeasonController {

    private final FarmingSeasonService farmingSeasonService;

    public FarmingSeasonController(FarmingSeasonService farmingSeasonService) {
        this.farmingSeasonService = farmingSeasonService;
    }

    /**
     * BICAP-13 (list): Lấy danh sách mùa vụ của farm, phân trang.
     */
    @GetMapping
    public ResponseEntity<Page<SeasonResponse>> getSeasons(
            @PathVariable Long farmId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(farmingSeasonService.getSeasons(farmId, pageable));
    }

    /**
     * BICAP-13 (detail): Lấy chi tiết mùa vụ kèm toàn bộ quy trình.
     */
    @GetMapping("/{seasonId}")
    public ResponseEntity<SeasonResponse> getSeasonDetail(
            @PathVariable Long farmId,
            @PathVariable Long seasonId) {
        return ResponseEntity.ok(farmingSeasonService.getSeasonDetail(farmId, seasonId));
    }

    /**
     * BICAP-14: Tạo mùa vụ mới và ghi lên Blockchain.
     */
    @PostMapping
    public ResponseEntity<SeasonResponse> createSeason(
            @PathVariable Long farmId,
            @Valid @RequestBody SeasonRequest request) {
        SeasonResponse created = farmingSeasonService.createSeason(farmId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * BICAP-12: Lấy danh sách quy trình của mùa vụ theo thứ tự thời gian.
     */
    @GetMapping("/{seasonId}/processes")
    public ResponseEntity<List<ProcessResponse>> getProcesses(
            @PathVariable Long farmId,
            @PathVariable Long seasonId) {
        return ResponseEntity.ok(farmingSeasonService.getProcesses(farmId, seasonId));
    }

    /**
     * BICAP-15: Thêm bước quy trình vào mùa vụ và ghi lên Blockchain.
     */
    @PostMapping("/{seasonId}/processes")
    public ResponseEntity<ProcessResponse> addProcess(
            @PathVariable Long farmId,
            @PathVariable Long seasonId,
            @Valid @RequestBody ProcessRequest request) {
        ProcessResponse created = farmingSeasonService.addProcess(farmId, seasonId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{seasonId}/processes/{processId}")
    public ResponseEntity<ProcessResponse> updateProcess(
            @PathVariable Long farmId,
            @PathVariable Long seasonId,
            @PathVariable Long processId,
            @Valid @RequestBody ProcessRequest request) {
        return ResponseEntity.ok(farmingSeasonService.updateProcess(farmId, seasonId, processId, request));
    }
}
