package vn.courses.ut.edu.javaprogramming.bicap;

import org.junit.jupiter.api.Test;
import vn.courses.ut.edu.javaprogramming.bicap.common.blockchain.HexUtils;
import vn.courses.ut.edu.javaprogramming.bicap.common.blockchain.RlpEncoder;
import vn.courses.ut.edu.javaprogramming.bicap.common.blockchain.VeChainTxSigner;
import vn.courses.ut.edu.javaprogramming.bicap.common.blockchain.VeChainWallet;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BICAP-74/80/89 — unit tests for the VeChainThor signing primitives:
 * RLP vectors, deterministic ECDSA (RFC 6979) with recovery id, address derivation.
 */
class VeChainCryptoTest {

    // ── RLP known vectors (ethereum wiki) ──────────────────────────────────
    @Test
    void rlp_knownVectors() {
        assertArrayEquals(new byte[]{(byte) 0x83, 0x64, 0x6f, 0x67},
                RlpEncoder.encodeBytes("dog".getBytes(StandardCharsets.UTF_8)));
        assertArrayEquals(new byte[]{(byte) 0x80}, RlpEncoder.encodeBytes(new byte[0]));
        assertArrayEquals(new byte[]{0x0f}, RlpEncoder.encodeLong(15));
        assertArrayEquals(new byte[]{(byte) 0x82, 0x04, 0x00}, RlpEncoder.encodeLong(1024));
        assertArrayEquals(new byte[]{(byte) 0x80}, RlpEncoder.encodeLong(0));

        byte[] list = RlpEncoder.encodeList(List.of(
                RlpEncoder.encodeBytes("cat".getBytes(StandardCharsets.UTF_8)),
                RlpEncoder.encodeBytes("dog".getBytes(StandardCharsets.UTF_8))));
        assertArrayEquals(new byte[]{(byte) 0xc8, (byte) 0x83, 'c', 'a', 't', (byte) 0x83, 'd', 'o', 'g'}, list);
    }

    @Test
    void rlp_longStringUsesIndirectHeader() {
        byte[] longBytes = new byte[1024];
        java.util.Arrays.fill(longBytes, (byte) 'a');
        byte[] encoded = RlpEncoder.encodeBytes(longBytes);
        assertEquals((byte) 0xb9, encoded[0]);  // 0xb7 + 2 length bytes (1024 = 0x0400)
        assertEquals(0x04, encoded[1] & 0xFF);
        assertEquals(0x00, encoded[2] & 0xFF);
        assertEquals(1024 + 3, encoded.length);
    }

    // ── Signing determinism & recovery ─────────────────────────────────────
    private static final byte[] TEST_KEY =
            HexUtils.fromHex("0x4646464646464646464646464646464646464646464646464646464646464646");

    @Test
    void sign_isDeterministic_andProducesCanonicalLowS() {
        List<VeChainTxSigner.Clause> clauses = List.of(
                VeChainTxSigner.Clause.call("0xf077b491bb1c5f53849f4d5082f1f638c0d0b689",
                        BigInteger.ZERO, HexUtils.fromHex("deadbeef")));

        VeChainTxSigner.SignedTransaction a = VeChainTxSigner.signType0(
                39, new byte[]{0, 0, 0, 0, 0x01, 0x02, 0x03, 0x04}, 720, clauses,
                128, 53000, 12345L, TEST_KEY);
        VeChainTxSigner.SignedTransaction b = VeChainTxSigner.signType0(
                39, new byte[]{0, 0, 0, 0, 0x01, 0x02, 0x03, 0x04}, 720, clauses,
                128, 53000, 12345L, TEST_KEY);

        assertArrayEquals(a.rawTx(), b.rawTx(), "RFC6979 signing must be deterministic");
        assertEquals(32, a.id().length);
        byte[] sig = extractSignature(a.rawTx());
        assertEquals(65, sig.length);
        assertTrue(sig[64] == 0 || sig[64] == 1, "recovery id must be 0/1");
        BigInteger s = new BigInteger(1, java.util.Arrays.copyOfRange(sig, 32, 64));
        BigInteger halfN = new BigInteger("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141", 16);
        assertTrue(s.compareTo(halfN) <= 0, "signature must use canonical low-s");
    }

    @Test
    void sign_differentNonceProducesDifferentId() {
        List<VeChainTxSigner.Clause> clauses = List.of(
                VeChainTxSigner.Clause.create(BigInteger.ZERO, new byte[]{1, 2, 3}));
        VeChainTxSigner.SignedTransaction a = VeChainTxSigner.signType0(
                39, new byte[8], 720, clauses, 128, 21000, 1L, TEST_KEY);
        VeChainTxSigner.SignedTransaction b = VeChainTxSigner.signType0(
                39, new byte[8], 720, clauses, 128, 21000, 2L, TEST_KEY);
        assertFalse(java.util.Arrays.equals(a.id(), b.id()));
    }

    // ── Wallet / address derivation ────────────────────────────────────────
    @Test
    void wallet_addressDerivation_isStableAndWellFormed() {
        String addr1 = VeChainWallet.addressFromPrivateKey(TEST_KEY);
        String addr2 = VeChainWallet.addressFromPrivateKey(TEST_KEY);
        assertEquals(addr1, addr2);
        assertTrue(addr1.matches("0x[0-9a-f]{40}"), "address must be 0x + 40 hex chars");
        assertNotEquals(addr1, VeChainWallet.addressFromPrivateKey(
                HexUtils.fromHex("0x1111111111111111111111111111111111111111111111111111111111111111")));
    }

    @Test
    void wallet_contractAddress_isStableAndWellFormed() {
        String sender = VeChainWallet.addressFromPrivateKey(TEST_KEY);
        String c1 = VeChainWallet.contractAddress(sender, 7L);
        String c2 = VeChainWallet.contractAddress(sender, 7L);
        assertEquals(c1, c2);
        assertTrue(c1.matches("0x[0-9a-f]{40}"));
        assertNotEquals(c1, VeChainWallet.contractAddress(sender, 8L));
    }

    /** Pulls the trailing 65-byte signature out of the encoded tx (test helper). */
    private static byte[] extractSignature(byte[] rawTx) {
        byte[] sig = new byte[65];
        System.arraycopy(rawTx, rawTx.length - 65, sig, 0, 65);
        return sig;
    }
}
