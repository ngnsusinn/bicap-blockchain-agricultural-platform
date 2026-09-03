package vn.courses.ut.edu.javaprogramming.bicap.common.blockchain;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Thin REST client for a VeChainThor node (thor). Implements exactly the endpoints the
 * platform needs for live attestation writes (BICAP-74 / SRS-API-003):
 *
 * <ul>
 *   <li>{@code GET /blocks/best} — connectivity, chain tag and block reference</li>
 *   <li>{@code POST /transactions} — broadcast a signed raw transaction</li>
 *   <li>{@code GET /transactions/{id}} — execution status (1 pending, 2 confirmed, 3 error)</li>
 *   <li>{@code GET /transactions/{id}/receipt} — receipt once executed (404 while pending)</li>
 * </ul>
 */
public class VeChainClient {

    /** txStatus values reported by thor. */
    public static final int TX_UNKNOWN = 0;
    public static final int TX_PENDING = 1;
    public static final int TX_CONFIRMED = 2;
    public static final int TX_ERROR = 3;

    public record BestBlock(long number, byte[] id) {}

    public record Receipt(boolean reverted, long gasUsed) {}

    private final String nodeUrl;
    private final RestTemplate restTemplate;

    public VeChainClient(String nodeUrl) {
        this.nodeUrl = nodeUrl.endsWith("/") ? nodeUrl.substring(0, nodeUrl.length() - 1) : nodeUrl;
        this.restTemplate = new RestTemplate();
    }

    public BestBlock getBestBlock() {
        Map<?, ?> body = restTemplate.getForObject(nodeUrl + "/blocks/best", Map.class);
        if (body == null || body.get("id") == null) {
            throw new IllegalStateException("Unexpected /blocks/best response from " + nodeUrl);
        }
        long number = ((Number) body.get("number")).longValue();
        return new BestBlock(number, HexUtils.fromHex(String.valueOf(body.get("id"))));
    }

    /**
     * Chain tag = last byte of the GENESIS block id (constant per network:
     * mainnet 0x4a, testnet 0x27, ...). Must NOT be taken from the best block —
     * only block 0's id encodes the network tag.
     */
    public int getChainTag() {
        Map<?, ?> genesis = restTemplate.getForObject(nodeUrl + "/blocks/0", Map.class);
        if (genesis == null || genesis.get("id") == null) {
            throw new IllegalStateException("Unexpected /blocks/0 response from " + nodeUrl);
        }
        byte[] id = HexUtils.fromHex(String.valueOf(genesis.get("id")));
        return id[id.length - 1] & 0xFF;
    }

    /** Broadcasts a signed raw transaction and returns its id (0x…64). */
    public String sendRawTransaction(byte[] rawTx) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request =
                new HttpEntity<>(Map.of("raw", HexUtils.toHex(rawTx)), headers);
        Map<?, ?> response = restTemplate.postForObject(nodeUrl + "/transactions", request, Map.class);
        if (response == null || response.get("id") == null) {
            throw new IllegalStateException("Node did not accept the transaction");
        }
        return String.valueOf(response.get("id"));
    }

    public int getTransactionStatus(String txId) {
        try {
            Map<?, ?> body = restTemplate.getForObject(nodeUrl + "/transactions/" + txId, Map.class);
            Object status = body == null ? null : body.get("txStatus");
            return status == null ? TX_UNKNOWN : ((Number) status).intValue();
        } catch (HttpClientErrorException.NotFound e) {
            return TX_UNKNOWN;
        }
    }

    /** Returns the receipt, or {@code null} while the transaction is still pending. */
    public Receipt getReceipt(String txId) {
        try {
            Map<?, ?> body = restTemplate.getForObject(nodeUrl + "/transactions/" + txId + "/receipt", Map.class);
            if (body == null) {
                return null;
            }
            boolean reverted = Boolean.TRUE.equals(body.get("reverted"));
            Object gas = body.get("gasUsed");
            long gasUsed = gas instanceof Number num ? num.longValue() : 0L;
            return new Receipt(reverted, gasUsed);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }

    public boolean isHealthy() {
        try {
            getBestBlock();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
