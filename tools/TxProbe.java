import vn.courses.ut.edu.javaprogramming.bicap.common.blockchain.HexUtils;
import vn.courses.ut.edu.javaprogramming.bicap.common.blockchain.VeChainTxSigner;
import vn.courses.ut.edu.javaprogramming.bicap.common.blockchain.VeChainWallet;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/** Pure-JDK probe: build + broadcast a type-0 tx, print exactly what the node says. */
public class TxProbe {
    static final String NODE = "http://localhost:8669";

    public static void main(String[] args) throws Exception {
        byte[] priv = HexUtils.fromHex(args[0]);
        HttpClient node = HttpClient.newBuilder().build();
        int chainTag = chainTag(node);

        long nonce = (System.currentTimeMillis() & 0xFFFFFFFFL);
        String self = VeChainWallet.addressFromPrivateKey(priv);

        int[][] configs = {{0, 21000, 0}, {128, 21000, 0}, {255, 21000, 0}, {0, 53000, 1}};
        for (int[] cfg : configs) {
            int coef = cfg[0]; long gas = cfg[1]; boolean withData = cfg[2] == 1;
            nonce++;
            System.out.println("=== coef=" + coef + " gas=" + gas + " data=" + withData + " ===");
            List<VeChainTxSigner.Clause> cs = withData
                    ? List.of(VeChainTxSigner.Clause.call(self, BigInteger.ZERO, HexUtils.fromHex("deadbeef")))
                    : List.of(VeChainTxSigner.Clause.call(self, BigInteger.ZERO, new byte[0]));
            long blockNumber = blockNumber(node);
            VeChainTxSigner.SignedTransaction s =
                    VeChainTxSigner.signType0(chainTag, blockNumber, 10000, cs, coef, gas, nonce, priv);
            HttpRequest req = HttpRequest.newBuilder(URI.create(NODE + "/transactions"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"raw\":\"" + HexUtils.toHex(s.rawTx()) + "\"}"))
                    .build();
            HttpResponse<String> resp = node.send(req, HttpResponse.BodyHandlers.ofString());
            System.out.println("HTTP " + resp.statusCode() + " -> " + resp.body().trim());
        }
    }

    static int chainTag(HttpClient node) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(NODE + "/blocks/0")).GET().build();
        String body = node.send(req, HttpResponse.BodyHandlers.ofString()).body();
        int idx = body.indexOf("\"id\":\"");
        int start = idx + 6;
        int end = body.indexOf('"', start);
        String id = body.substring(start, end);
        System.out.println("genesis id=" + id + " len=" + id.length());
        return Integer.parseInt(id.substring(id.length() - 2), 16);
    }

    static long blockNumber(HttpClient node) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(NODE + "/blocks/best")).GET().build();
        String body = node.send(req, HttpResponse.BodyHandlers.ofString()).body();
        int idx = body.indexOf("\"number\":");
        int end = body.indexOf(',', idx + 9);
        return Long.parseLong(body.substring(idx + 9, end).trim());
    }
}
