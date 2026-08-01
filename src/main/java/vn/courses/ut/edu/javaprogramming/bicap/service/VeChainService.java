package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import vn.courses.ut.edu.javaprogramming.bicap.dto.BlockchainTransactionResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.BlockchainWriteRequest;
import java.util.Map;

@Service
public class VeChainService {

    private final WebClient webClient;

    public VeChainService(@Value("${vechain.node.url}") String nodeUrl) {
        // Khởi tạo WebClient với URL của VeChain Node (Mainnet hoặc Testnet)
        this.webClient = WebClient.builder()
                .baseUrl(nodeUrl)
                .build();
    }

    /**
     * Lấy chi tiết của một giao dịch dựa vào txId
     * API: GET /transactions/{txId}
     */
    public Map<String, Object> getTransactionDetail(String txId) {
        try {
            // Dùng WebClient để gọi HTTP GET tới VeChain Node
            return webClient.get()
                    .uri("/transactions/{id}", txId)
                    .retrieve()
                    .bodyToMono(Map.class) // Đọc JSON trả về thành một Map (từ điển)
                    .block(); // Đợi kết quả (block) vì mình đang viết theo dạng đồng bộ
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy thông tin giao dịch từ VeChain: " + e.getMessage(), e);
        }
    }

    /**
     * Ghi một giao dịch lên VeChain.
     * Lưu ý: Hiện tại để đơn giản hóa quá trình ký (sign transaction), 
     * mình giả lập hàm này sẽ trả về một fake txId.
     * Trong thực tế, bạn cần build Raw Transaction và dùng private key 
     * (thường là secp256k1) để ký locally trước khi POST lên /transactions.
     */
    public BlockchainTransactionResponse writeDataToBlockchain(BlockchainWriteRequest request) {
        // Chỗ này cần logic tạo transaction, mã hoá data (dạng hex), và ký bằng private key.
        // Đây là phần khó nhất khi làm việc với VeChainThor bằng raw HTTP (không có SDK).
        
        // GIẢ LẬP KẾT QUẢ GHI THÀNH CÔNG:
        String fakeTxId = "0x" + java.util.UUID.randomUUID().toString().replace("-", "") 
                        + java.util.UUID.randomUUID().toString().replace("-", ""); // tạo chuỗi hex 64 kí tự
        
        return new BlockchainTransactionResponse(
                fakeTxId, 
                "SUCCESS", 
                "Dữ liệu đã được ghi nhận. (Chú ý: Đây là giả lập vì bước ký transaction yêu cầu thư viện crypto phức tạp)."
        );
    }
}
