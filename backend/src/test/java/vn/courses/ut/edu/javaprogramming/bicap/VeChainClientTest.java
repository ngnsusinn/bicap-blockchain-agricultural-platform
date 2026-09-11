package vn.courses.ut.edu.javaprogramming.bicap;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import vn.courses.ut.edu.javaprogramming.bicap.common.blockchain.VeChainClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression for the live-confirmation path.
 *
 * <p>{@code BlockchainMaintenanceJob} confirms PENDING broadcasts by polling
 * {@link VeChainClient#getTransactionStatus}. That method used to read a {@code txStatus}
 * field which thor does not return on {@code GET /transactions/{id}}, so every broadcast
 * stayed PENDING forever and its hash was never written back to the entity. These tests
 * pin the real thor contract (404 / no meta / meta.blockID + receipt) against a stub node.
 */
class VeChainClientTest {

    private static HttpServer server;
    private static String baseUrl;

    /** Routes: /transactions/notfound → 404, /pending → tx without meta, /mined → tx with meta. */
    @BeforeAll
    static void startStubNode() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/transactions/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String rest = path.substring("/transactions/".length());
            boolean isReceipt = rest.endsWith("/receipt");
            String id = isReceipt ? rest.substring(0, rest.length() - "/receipt".length()) : rest;

            String body;
            int status = 200;
            if (isReceipt) {
                if ("0xmined".equals(id)) {
                    body = "{\"gasUsed\":23176,\"reverted\":false,\"paid\":\"0x337607df9250000\"}";
                } else if ("0xreverted".equals(id)) {
                    body = "{\"gasUsed\":23176,\"reverted\":true,\"paid\":\"0x337607df9250000\"}";
                } else {
                    status = 404;
                    body = "{\"error\":\"not found\"}";
                }
            } else if ("0xpending".equals(id)) {
                body = "{\"id\":\"0xpending\",\"gas\":53000,\"origin\":\"0xabc\"}";
            } else if ("0xmined".equals(id) || "0xreverted".equals(id)) {
                body = "{\"id\":\"" + id + "\",\"gas\":53000,\"meta\":{\"blockID\":\"0x018b58d97cf6b485\"}}";
            } else {
                status = 404;
                body = "{\"error\":\"not found\"}";
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopStubNode() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void unknownTransaction_isUnknown_notPending() {
        VeChainClient client = new VeChainClient(baseUrl);
        assertEquals(VeChainClient.TX_UNKNOWN, client.getTransactionStatus("0xnotfound"));
    }

    @Test
    void transactionWithoutBlockMeta_isPending() {
        VeChainClient client = new VeChainClient(baseUrl);
        assertEquals(VeChainClient.TX_PENDING, client.getTransactionStatus("0xpending"));
    }

    @Test
    void transactionWithBlockMetaAndCleanReceipt_isConfirmed() {
        VeChainClient client = new VeChainClient(baseUrl);
        assertEquals(VeChainClient.TX_CONFIRMED, client.getTransactionStatus("0xmined"));
    }

    @Test
    void transactionWithBlockMetaAndRevertedReceipt_isError() {
        VeChainClient client = new VeChainClient(baseUrl);
        assertEquals(VeChainClient.TX_ERROR, client.getTransactionStatus("0xreverted"));
    }

    @Test
    void missingReceipt_returnsNull() {
        VeChainClient client = new VeChainClient(baseUrl);
        assertNull(client.getReceipt("0xpending"));
    }
}
