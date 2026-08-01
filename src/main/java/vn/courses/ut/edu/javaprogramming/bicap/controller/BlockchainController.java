package vn.courses.ut.edu.javaprogramming.bicap.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.dto.BlockchainTransactionResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.BlockchainWriteRequest;
import vn.courses.ut.edu.javaprogramming.bicap.service.VeChainService;

import java.util.Map;

@RestController
@RequestMapping("/api/blockchain")
public class BlockchainController {

    @Autowired
    private VeChainService veChainService;

    /**
     * API 1: Ghi dữ liệu lên Blockchain
     * POST /api/blockchain/transactions
     */
    @PostMapping("/transactions")
    public ResponseEntity<BlockchainTransactionResponse> writeToBlockchain(
            @Validated @RequestBody BlockchainWriteRequest request) {
        
        // Gọi service để xử lý logic ghi blockchain
        BlockchainTransactionResponse response = veChainService.writeDataToBlockchain(request);
        
        return ResponseEntity.ok(response);
    }

    /**
     * API 2: Lấy thông tin giao dịch từ Blockchain
     * GET /api/blockchain/transactions/{txId}
     */
    @GetMapping("/transactions/{txId}")
    public ResponseEntity<Map<String, Object>> getTransactionDetail(@PathVariable String txId) {
        
        // Gọi service đọc API của VeChain Node
        Map<String, Object> transactionInfo = veChainService.getTransactionDetail(txId);
        
        // Nếu không tìm thấy giao dịch (trả về null)
        if (transactionInfo == null || transactionInfo.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(transactionInfo);
    }
}
