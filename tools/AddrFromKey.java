import vn.courses.ut.edu.javaprogramming.bicap.common.blockchain.HexUtils;
import vn.courses.ut.edu.javaprogramming.bicap.common.blockchain.VeChainWallet;

public class AddrFromKey {
    public static void main(String[] args) {
        byte[] key = HexUtils.fromHex(args[0]);
        if (key.length != 32) {
            System.out.println("SAU: private key phai dung 32 bytes (64 ky tu hex), nay = " + key.length);
            System.exit(1);
        }
        System.out.println("DIA CHI VI: " + VeChainWallet.addressFromPrivateKey(key));
    }
}
