package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.dto.DeployContractRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.UpdateContractRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.SmartContract;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.BlockchainService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin smart-contract lifecycle (BICAP-6 / SRS-ADM-005).
 *
 * <p>F6 fix: the module previously only listed and deployed — the UI's "update / manage"
 * actions did not exist. Deploy (SUPER_ADMIN), read (admin view), update metadata and
 * change lifecycle status (admin write) are all supported now, and
 * {@code GET /blockchain-status} exposes the real anchoring mode so the UI can stop
 * claiming a VeChainThor deployment when the platform runs in mock mode.
 */
@RestController
@RequestMapping("/api/admin/contracts")
public class SmartContractController {

    private final BlockchainService blockchainService;
    private final UserRepository userRepository;

    public SmartContractController(BlockchainService blockchainService, UserRepository userRepository) {
        this.blockchainService = blockchainService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<SmartContract>> getContracts(
            @RequestHeader("X-Actor-Email") String actorEmail) {
        ActorAuthorizer.requireAdminView(userRepository, actorEmail);
        List<SmartContract> contracts = blockchainService.getContracts();
        return ResponseEntity.ok(contracts);
    }

    /** Runtime blockchain mode — used by the admin UI to label deploys honestly. */
    @GetMapping("/blockchain-status")
    public ResponseEntity<Map<String, Object>> blockchainStatus(
            @RequestHeader("X-Actor-Email") String actorEmail) {
        ActorAuthorizer.requireAdminView(userRepository, actorEmail);
        Map<String, Object> status = new LinkedHashMap<>();
        boolean live = blockchainService.isLive();
        status.put("mode", live ? "live" : "mock");
        status.put("live", live);
        status.put("message", live
                ? "Giao dịch được ký và broadcast thật lên VeChainThor."
                : "Chế độ mô phỏng: hash được sinh cục bộ, không có giao dịch on-chain thật.");
        return ResponseEntity.ok(status);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SmartContract> getContract(
            @RequestHeader("X-Actor-Email") String actorEmail,
            @PathVariable Long id) {
        ActorAuthorizer.requireAdminView(userRepository, actorEmail);
        return ResponseEntity.ok(blockchainService.getContract(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SmartContract> updateContract(
            @RequestHeader("X-Actor-Email") String actorEmail,
            @PathVariable Long id,
            @Valid @RequestBody UpdateContractRequest request) {
        ActorAuthorizer.requireAdminWrite(userRepository, actorEmail);
        return ResponseEntity.ok(blockchainService.updateContract(id, request));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<SmartContract> updateContractStatus(
            @RequestHeader("X-Actor-Email") String actorEmail,
            @PathVariable Long id,
            @Valid @RequestBody UpdateContractRequest request) {
        ActorAuthorizer.requireAdminWrite(userRepository, actorEmail);
        return ResponseEntity.ok(blockchainService.updateContractStatus(id, request.getStatus()));
    }

    @PostMapping("/deploy")
    public ResponseEntity<SmartContract> deployContract(
            @RequestHeader("X-Actor-Email") String actorEmail,
            @Valid @RequestBody DeployContractRequest request) {
        ActorAuthorizer.requireSuperAdmin(userRepository, actorEmail);
        SmartContract contract = blockchainService.deployContract(
                request.getName(),
                request.getBytecode(),
                request.getAbi(),
                request.getEnvironment(),
                request.getVersion()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(contract);
    }
}
