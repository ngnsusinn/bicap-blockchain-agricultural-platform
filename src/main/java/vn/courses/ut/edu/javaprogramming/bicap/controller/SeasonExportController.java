package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.dto.SeasonExportRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.SeasonExportResponse;
import vn.courses.ut.edu.javaprogramming.bicap.service.SeasonExportService;
import java.util.List;

@RestController
public class SeasonExportController {
    private final SeasonExportService service;
    public SeasonExportController(SeasonExportService service) { this.service = service; }

    @PostMapping("/api/farms/{farmId}/seasons/{seasonId}/export")
    public ResponseEntity<SeasonExportResponse> create(@PathVariable Long farmId, @PathVariable Long seasonId,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody SeasonExportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(farmId, seasonId, request, idempotencyKey));
    }

    @GetMapping("/api/farms/{farmId}/exports")
    public List<SeasonExportResponse> list(@PathVariable Long farmId) { return service.list(farmId); }

    @GetMapping("/api/trace/{hash}")
    public SeasonExportResponse trace(@PathVariable String hash) { return service.trace(hash); }
}
