package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ProcessCreateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ProcessResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ProcessUpdateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingProcess;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.service.ProcessService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/seasons/{seasonId}/processes")
public class FarmingProcessController {

    private final ProcessService processService;

    public FarmingProcessController(ProcessService processService) {
        this.processService = processService;
    }

    @PostMapping
    public ResponseEntity<ProcessResponse> addProcess(
            @PathVariable Long seasonId,
            @Valid @RequestBody ProcessCreateRequest request) {
        User user = CurrentUser.get();
        FarmingProcess process = processService.addProcess(seasonId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(process));
    }

    @GetMapping
    public ResponseEntity<List<ProcessResponse>> getProcesses(
            @PathVariable Long seasonId) {
        User user = CurrentUser.get();
        List<FarmingProcess> processes = processService.getProcessesBySeason(seasonId, user);
        List<ProcessResponse> response = processes.stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{processId}")
    public ResponseEntity<ProcessResponse> getProcess(
            @PathVariable Long seasonId,
            @PathVariable Long processId) {
        User user = CurrentUser.get();
        FarmingProcess process = processService.getProcess(seasonId, processId, user);
        return ResponseEntity.ok(toResponse(process));
    }

    @PutMapping("/{processId}")
    public ResponseEntity<ProcessResponse> updateProcess(
            @PathVariable Long seasonId,
            @PathVariable Long processId,
            @Valid @RequestBody ProcessUpdateRequest request) {
        User user = CurrentUser.get();
        FarmingProcess process = processService.updateProcess(seasonId, processId, request, user);
        return ResponseEntity.ok(toResponse(process));
    }

    private ProcessResponse toResponse(FarmingProcess p) {
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
}
