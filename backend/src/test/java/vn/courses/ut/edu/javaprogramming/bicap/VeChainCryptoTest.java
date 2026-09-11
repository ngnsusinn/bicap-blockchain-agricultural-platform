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
                39, 16909060L, 720, clauses,
                128, 53000, 12345L, TEST_KEY);
        VeChainTxSigner.SignedTransaction b = VeChainTxSigner.signType0(
                39, 16909060L, 720, clauses,
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
                39, 0L, 720, clauses, 128, 21000, 1L, TEST_KEY);
        VeChainTxSigner.SignedTransaction b = VeChainTxSigner.signType0(
                39, 0L, 720, clauses, 128, 21000, 2L, TEST_KEY);
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

    // ── BlockRef (live-broadcast regression) ───────────────────────────────
    //
    // Live testnet broadcast failed with "403 tx rejected: expired" because the signer
    // was given the block HEIGHT instead of the 8-byte block id prefix. thor derives the
    // reference number from the HIGH 4 bytes of blockRef, so a bare height (0x00000000…) 
    // resolved to block 0 and every transaction was instantly expired.

    /** Real testnet block id observed while debugging (block 25909416). */
    private static final String REAL_TESTNET_BLOCK_ID =
            "0x018b58a88f6b3bb2605a4b1842079363bcdc2c3e4ca053af54a15d2f927814c4";

    @Test
    void blockRef_isFirstEightBytesOfBlockId_notBlockHeight() {
        long ref = VeChainTxSigner.blockRefFromBlockId(HexUtils.fromHex(REAL_TESTNET_BLOCK_ID));

        assertEquals(0x018b58a88f6b3bb2L, ref, "blockRef must be blockId[0..8] as uint64");
        // The reference block height lives in the high 4 bytes — this is why the bare
        // height never worked (its high 4 bytes are zero).
        assertEquals(25909416L, ref >>> 32, "high 4 bytes must encode the reference block number");
    }

    @Test
    void blockRef_isEncodedAsEightByteRlpInteger() {
        long ref = VeChainTxSigner.blockRefFromBlockId(HexUtils.fromHex(REAL_TESTNET_BLOCK_ID));
        byte[] encoded = RlpEncoder.encodeBigInteger(BigInteger.valueOf(ref));

        // 8 data bytes → 0x88 header, then the exact block id prefix.
        assertEquals(9, encoded.length, "blockRef must encode as an 8-byte integer");
        assertEquals((byte) 0x88, encoded[0]);
        assertEquals("018b58a88f6b3bb2",
                HexUtils.toHex(java.util.Arrays.copyOfRange(encoded, 1, 9)).substring(2));
    }

    @Test
    void blockRef_handlesHighBitAsUnsigned() {
        // A block id whose first byte is >= 0x80 yields a negative Java long; it must still
        // serialize as 8 unsigned bytes rather than throwing in the RLP encoder.
        byte[] blockId = HexUtils.fromHex("0xff01020304050607" + "00".repeat(24));
        long ref = VeChainTxSigner.blockRefFromBlockId(blockId);
        assertTrue(ref < 0, "top byte 0xff makes the signed long negative");

        List<VeChainTxSigner.Clause> clauses = List.of(
                VeChainTxSigner.Clause.call("0xf077b491bb1c5f53849f4d5082f1f638c0d0b689",
                        BigInteger.ZERO, new byte[0]));
        VeChainTxSigner.SignedTransaction tx = VeChainTxSigner.signType0(
                39, ref, 720, clauses, 0, 53000, 1L, TEST_KEY);

        assertNotNull(tx.rawTx());
        assertTrue(HexUtils.toHex(tx.rawTx()).contains("88ff01020304050607"),
                "blockRef must be encoded as the 8 unsigned bytes");
    }

    // ── Golden vector from the official VeChain SDK (thor-devkit) ──────────
    //
    // Live broadcast failed with "403 insufficient energy" even though the wallet held
    // VTHO: we hashed a 10-field payload (signature included, empty) instead of the
    // 9-field payload thor expects, so thor recovered a different, unfunded sender.
    // These constants were produced by thor-devkit for exactly these inputs; if our raw
    // transaction stops matching byte-for-byte, live broadcasting breaks again.

    @Test
    void signType0_matchesOfficialSdkGoldenVector() {
        List<VeChainTxSigner.Clause> clauses = List.of(
                VeChainTxSigner.Clause.call("0xf077b491bb1c5f53849f4d5082f1f638c0d0b689",
                        BigInteger.ZERO, HexUtils.fromHex("deadbeef")));

        VeChainTxSigner.SignedTransaction tx = VeChainTxSigner.signType0(
                39, 0x018b58c9e60bd7e4L, 720, clauses, 128, 53000, 12345L, TEST_KEY);

        assertEquals("0x7c4ca0c4950dbb5b563783fafbe7d4d734eb012381b7c5aa4a488562b22d22cf",
                HexUtils.toHex(tx.id()), "signing hash must cover the 9-field payload (no signature)");
        assertEquals(
                "0xf8772788018b58c9e60bd7e48202d0dcdb94f077b491bb1c5f53849f4d5082f1f638c0d0b689"
                        + "8084deadbeef818082cf0880823039c0b841"
                        + "a95ff1a151d1987df34e8acdaa4e53f99c9f54411d230463580d25f12b7856af"
                        + "1d2d42b39632aed80f316c8c548e324aa1b095301835b1c7f20286d77918c8fb00",
                HexUtils.toHex(tx.rawTx()), "raw tx must be byte-identical to the official SDK");

        // The signer of that key is 0x9d8a…5a4f in thor-devkit; the id above is what thor
        // hashes to recover it, so a matching id proves recovery lands on the funded wallet.
        assertEquals("0x9d8a62f656a8d1615c1294fd71e9cfb3e4855a4f",
                VeChainWallet.addressFromPrivateKey(TEST_KEY));
    }

    /** Pulls the trailing 65-byte signature out of the encoded tx (test helper). */
    private static byte[] extractSignature(byte[] rawTx) {
        byte[] sig = new byte[65];
        System.arraycopy(rawTx, rawTx.length - 65, sig, 0, 65);
        return sig;
    }
}
