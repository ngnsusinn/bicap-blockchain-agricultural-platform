package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.dto.DeployContractRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.SmartContract;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.BlockchainService;

import java.util.List;

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
