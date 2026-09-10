import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.math.ec.ECPoint;
import vn.courses.ut.edu.javaprogramming.bicap.common.blockchain.HexUtils;
import vn.courses.ut.edu.javaprogramming.bicap.common.blockchain.VeChainWallet;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Arrays;

/**
 * Derives the VeChainThor private key from a BIP39 mnemonic (12 words) using the
 * Sync2/VeChain standard account path m/44'/818'/0'/0/i, scans i = 0..19 and,
 * when a target address is supplied, writes the matching key into .env.
 *
 * SECURITY: the mnemonic is read from a LOCAL file only and the private key is
 * written straight to .env — neither is ever printed to stdout.
 *
 * Usage:
 *   java -cp "target/classes;<bc>" tools/MnemonicToKey.java [mnemonicFile] [targetAddress]
 *   (defaults: mnemonic.txt, no target = print derived addresses only)
 */
public final class MnemonicToKey {

    private static final org.bouncycastle.asn1.x9.X9ECParameters CURVE_PARAMS =
            CustomNamedCurves.getByName("secp256k1");
    private static final ECDomainParameters CURVE = new ECDomainParameters(
            CURVE_PARAMS.getCurve(), CURVE_PARAMS.getG(), CURVE_PARAMS.getN(), CURVE_PARAMS.getH());

    public static void main(String[] args) throws Exception {
        Path mnemonicFile = Path.of(args.length > 0 ? args[0] : "mnemonic.txt");
        String target = args.length > 1 ? args[1].toLowerCase() : null;

        if (!Files.exists(mnemonicFile)) {
            System.out.println("Khong thay file " + mnemonicFile.toAbsolutePath());
            System.out.println("Tao file mnemonic.txt chua 12 tu (cach nhau boi khoang trang/xeo) ROI chay lai.");
            return;
        }
        String mnemonic = Normalizer.normalize(Files.readString(mnemonicFile).trim(), Normalizer.Form.NFKD);

        byte[] seed = bip39Seed(mnemonic, "");
        byte[][] master = masterKey(seed);

        for (int i = 0; i < 20; i++) {
            byte[][] key = derivePath(master, new long[]{44 + HARDENED, 818 + HARDENED, 0 + HARDENED, 0, i});
            String address = VeChainWallet.addressFromPrivateKey(key[0]).toLowerCase();
            if (target == null) {
                System.out.println("m/44'/818'/0'/0/" + i + " -> " + address);
            } else if (address.equals(target)) {
                writeKeyToEnv(key[0]);
                System.out.println("KHOP: tai khoan " + i + " (" + address + ")");
                System.out.println("Da ghi private key tuong ung vao .env (dong BLOCKCHAIN_PRIVATE_KEY).");
                return;
            }
        }
        System.out.println("Khong tim thay dia chi " + target + " trong 20 tai khoan dau cua duong dan m/44'/818'/0'/0/i");
    }

    private static final long HARDENED = 0x80000000L;

    private static byte[] bip39Seed(String mnemonic, String passphrase) {
        byte[] salt = ("mnemonic" + passphrase).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        PKCS5S2ParametersGenerator kdf = new PKCS5S2ParametersGenerator(new SHA512Digest());
        kdf.init(mnemonic.getBytes(java.nio.charset.StandardCharsets.UTF_8), salt, 2048);
        return ((KeyParameter) kdf.generateDerivedParameters(512)).getKey();
    }

    private static byte[][] masterKey(byte[] seed) {
        byte[] i = hmac512("Bitcoin seed".getBytes(java.nio.charset.StandardCharsets.UTF_8), seed);
        return new byte[][]{Arrays.copyOf(i, 32), Arrays.copyOfRange(i, 32, 64)};
    }

    private static byte[][] derivePath(byte[][] parent, long[] path) {
        byte[][] node = parent;
        for (long segment : path) {
            node = ckdPriv(node, segment);
        }
        return node;
    }

    private static byte[][] ckdPriv(byte[][] parent, long index) {
        byte[] kPar = parent[0];
        byte[] cPar = parent[1];
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        if ((index & HARDENED) != 0) {
            data.write(0);
            data.writeBytes(kPar);
        } else {
            data.writeBytes(publicPoint(kPar).getEncoded(true));
        }
        data.write((int) ((index >>> 24) & 0xFF));
        data.write((int) ((index >>> 16) & 0xFF));
        data.write((int) ((index >>> 8) & 0xFF));
        data.write((int) (index & 0xFF));

        byte[] i = hmac512(cPar, data.toByteArray());
        BigInteger il = new BigInteger(1, Arrays.copyOf(i, 32));
        BigInteger ki = il.add(new BigInteger(1, kPar)).mod(CURVE.getN());
        if (il.compareTo(CURVE.getN()) >= 0 || ki.signum() == 0) {
            throw new IllegalStateException("Invalid derived key (retry next index)");
        }
        return new byte[][]{toFixed(ki, 32), Arrays.copyOfRange(i, 32, 64)};
    }

    private static ECPoint publicPoint(byte[] privateKey) {
        return CURVE.getG().multiply(new BigInteger(1, privateKey)).normalize();
    }

    private static byte[] toFixed(BigInteger v, int len) {
        byte[] b = v.toByteArray();
        byte[] out = new byte[len];
        if (b.length >= len) {
            System.arraycopy(b, b.length - len, out, 0, len);
        } else {
            System.arraycopy(b, 0, out, len - b.length, b.length);
        }
        return out;
    }

    private static byte[] hmac512(byte[] key, byte[] data) {
        HMac mac = new HMac(new SHA512Digest());
        mac.init(new KeyParameter(key));
        mac.update(data, 0, data.length);
        byte[] out = new byte[64];
        mac.doFinal(out, 0);
        return out;
    }

    private static void writeKeyToEnv(byte[] privateKey) throws Exception {
        Path env = Path.of(".env");
        String hex = HexUtils.toHex(privateKey);
        if (Files.exists(env)) {
            String content = Files.readString(env);
            content = content.replaceAll("(?m)^BLOCKCHAIN_PRIVATE_KEY=.*$", "BLOCKCHAIN_PRIVATE_KEY=" + hex);
            Files.writeString(env, content);
        } else {
            Files.writeString(env, "BLOCKCHAIN_PRIVATE_KEY=" + hex + "\n");
        }
    }
}
