package vn.courses.ut.edu.javaprogramming.bicap.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.entity.BlockchainTransaction;
import vn.courses.ut.edu.javaprogramming.bicap.repository.BlockchainTransactionRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.BlockchainService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/blockchain/transactions")
public class BlockchainController {

    private final BlockchainService blockchainService;
    private final BlockchainTransactionRepository txRepository;
    private final UserRepository userRepository;

    public BlockchainController(BlockchainService blockchainService,
                                BlockchainTransactionRepository txRepository,
                                UserRepository userRepository) {
        this.blockchainService = blockchainService;
        this.txRepository = txRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<BlockchainTransaction>> getTransactions(
            @RequestHeader("X-Actor-Email") String actorEmail) {
        ActorAuthorizer.requireAdminView(userRepository, actorEmail);
        List<BlockchainTransaction> transactions = txRepository.findAll();
        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Map<String, Object>> retryTransaction(
            @RequestHeader("X-Actor-Email") String actorEmail,
            @PathVariable Long id) {
        ActorAuthorizer.requireAdminWrite(userRepository, actorEmail);
        boolean success = blockchainService.retryTransaction(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "Transaction completed and confirmed successfully" : "Transaction retry attempted but failed");
        return ResponseEntity.ok(response);
    }
}
