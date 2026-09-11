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

    /**
     * Execution status of a transaction, derived the way thor actually reports it.
     *
     * <p>The thor REST API has <b>no</b> {@code txStatus} field on
     * {@code GET /transactions/{id}} — reading it always yielded {@code TX_UNKNOWN}, so
     * PENDING broadcasts were never confirmed and their hash was never written back to the
     * season/process/export rows. The real signals are:
     * <ul>
     *   <li>404 → the node does not know the transaction ({@link #TX_UNKNOWN})</li>
     *   <li>200 without {@code meta.blockID} → still in the mempool ({@link #TX_PENDING})</li>
     *   <li>200 with {@code meta.blockID} → mined; the receipt's {@code reverted} flag then
     *       distinguishes {@link #TX_CONFIRMED} from {@link #TX_ERROR}</li>
     * </ul>
     */
    public int getTransactionStatus(String txId) {
        Map<?, ?> body;
        try {
            body = restTemplate.getForObject(nodeUrl + "/transactions/" + txId, Map.class);
        } catch (HttpClientErrorException.NotFound e) {
            return TX_UNKNOWN;
        }
        if (body == null) {
            return TX_UNKNOWN;
        }
        Object meta = body.get("meta");
        boolean mined = meta instanceof Map<?, ?> metaMap && metaMap.get("blockID") != null;
        if (!mined) {
            return TX_PENDING;
        }
        Receipt receipt = getReceipt(txId);
        if (receipt != null && receipt.reverted()) {
            return TX_ERROR;
        }
        return TX_CONFIRMED;
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
