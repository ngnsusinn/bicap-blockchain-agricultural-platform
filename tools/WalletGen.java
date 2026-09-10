import vn.courses.ut.edu.javaprogramming.bicap.common.blockchain.HexUtils;
import vn.courses.ut.edu.javaprogramming.bicap.common.blockchain.VeChainWallet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

/**
 * Sinh ví signer VeChainThor mới (testnet) và ghi trực tiếp vào .env.
 * Chay:  java -cp "target/classes;<bcprov.jar>" tools/WalletGen.java
 * In ra dia chi vi (nap VTHO tu faucet) — private key khong in ra man hinh.
 */
public class WalletGen {
    public static void main(String[] args) throws Exception {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        String privHex = HexUtils.toHex(key);
        String address = VeChainWallet.addressFromPrivateKey(key);

        Path env = Path.of(".env");
        if (Files.exists(env)) {
            String content = Files.readString(env);
            if (content.contains("BLOCKCHAIN_PRIVATE_KEY=")) {
                content = content.replaceAll("(?m)^BLOCKCHAIN_PRIVATE_KEY=.*$",
                        "BLOCKCHAIN_PRIVATE_KEY=" + privHex);
                Files.writeString(env, content);
                System.out.println("Da ghi private key vao .env (dong BLOCKCHAIN_PRIVATE_KEY).");
            } else {
                System.out.println(".env khong co dong BLOCKCHAIN_PRIVATE_KEY — tu them vao.");
            }
        } else {
            System.out.println("Khong thay .env — private key sinh ra: " + privHex);
        }

        System.out.println("============================================");
        System.out.println("DIA CHI VI SIGNER (dung de nap VTHO faucet):");
        System.out.println(address);
        System.out.println("============================================");
    }
}
