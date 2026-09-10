package vn.courses.ut.edu.javaprogramming.bicap.common.blockchain;

import java.math.BigInteger;

/** Derives the VeChainThor address belonging to a secp256k1 private key. */
public final class VeChainWallet {

    private VeChainWallet() {}

    /** address = last 20 bytes of keccak256(uncompressed pubkey without 0x04 prefix). */
    public static String addressFromPrivateKey(byte[] privateKey) {
        byte[] pub = VeChainTxSigner.publicKeyBytes(privateKey);
        byte[] hash = Hashes.keccak256(pub);
        byte[] address = new byte[20];
        System.arraycopy(hash, hash.length - 20, address, 0, 20);
        return HexUtils.toHex(address);
    }

    /**
     * VeChainThor contract address for a CREATE clause:
     * last 20 bytes of blake2b256(rlp([sender, nonce])).
     */
    public static String contractAddress(String senderAddress, long nonce) {
        byte[] sender = HexUtils.fromHex(senderAddress);
        byte[] encoded = RlpEncoder.encodeList(java.util.List.of(
                RlpEncoder.encodeBytes(sender),
                RlpEncoder.encodeLong(nonce)));
        byte[] hash = Hashes.blake2b256(encoded);
        byte[] address = new byte[20];
        System.arraycopy(hash, hash.length - 20, address, 0, 20);
        return HexUtils.toHex(address);
    }

    public static BigInteger privateKeyAsBigInt(byte[] privateKey) {
        return new BigInteger(1, privateKey);
    }
}
