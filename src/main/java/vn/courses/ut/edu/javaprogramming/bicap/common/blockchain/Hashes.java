package vn.courses.ut.edu.javaprogramming.bicap.common.blockchain;

import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.bouncycastle.crypto.digests.KeccakDigest;

/** Hash primitives used by VeChainThor: Blake2b-256 (tx hash) and Keccak-256 (ABI selectors). */
public final class Hashes {

    private Hashes() {}

    /** VeChainThor transaction / contract-address hashing. */
    public static byte[] blake2b256(byte[] input) {
        Blake2bDigest digest = new Blake2bDigest(256);
        digest.update(input, 0, input.length);
        byte[] out = new byte[32];
        digest.doFinal(out, 0);
        return out;
    }

    /** Ethereum/VeChain ABI function-selector hashing. */
    public static byte[] keccak256(byte[] input) {
        KeccakDigest digest = new KeccakDigest(256);
        digest.update(input, 0, input.length);
        byte[] out = new byte[32];
        digest.doFinal(out, 0);
        return out;
    }
}
