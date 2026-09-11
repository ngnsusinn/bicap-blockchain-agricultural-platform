package vn.courses.ut.edu.javaprogramming.bicap.common.blockchain;

import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.math.ec.ECAlgorithms;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds and signs VeChainThor legacy (type 0) transactions.
 *
 * <p>Broadcast payload (RLP):
 * {@code [ chainTag, blockRef, expiration, [[to, value, data]...], gasPriceCoef, gas,
 * dependsOn, nonce, reserved, signature ]}.
 *
 * <p>The signing hash — which is also the transaction id — is the Blake2b-256 of the
 * <b>9-field</b> payload with the signature field omitted. VeChainThor recovers the sender
 * from that hash, so hashing a 10-field payload with an empty signature (as Ethereum does)
 * makes thor recover a different, unfunded address and reject the broadcast.
 */
public final class VeChainTxSigner {

    private static final X9ECParameters SECP256K1 = CustomNamedCurves.getByName("secp256k1");
    private static final ECDomainParameters CURVE = new ECDomainParameters(
            SECP256K1.getCurve(), SECP256K1.getG(), SECP256K1.getN(), SECP256K1.getH());
    private static final BigInteger HALF_CURVE_ORDER = SECP256K1.getN().shiftRight(1);

    private VeChainTxSigner() {}

    /** One transaction clause: null {@code to} means contract creation. */
    public record Clause(byte[] to, BigInteger value, byte[] data) {
        public static Clause call(String toAddress, BigInteger value, byte[] data) {
            return new Clause(HexUtils.fromHex(toAddress), value, data);
        }
        public static Clause create(BigInteger value, byte[] bytecode) {
            return new Clause(null, value, bytecode);
        }
    }

    public record SignedTransaction(byte[] rawTx, byte[] id) {}

    /**
     * Legacy type-0 transaction (universally supported). Gas cost is expressed as a
     * {@code gasPriceCoef} uint8 (0..255); the node computes the actual price from it:
     * price = basePrice(1e11) + coef/255 * (maxPrice(1e13) - basePrice).
     * id = blake2b256(rlp(unsigned)).
     *
     * @param blockRef the 8-byte block reference as an unsigned uint64 — use
     *                 {@link #blockRefFromBlockId(byte[])}. It is NOT the plain block
     *                 number: thor reads the reference number from its <b>high</b> 4 bytes,
     *                 so passing the height alone makes every transaction look
     *                 {@code expired} and the node rejects it with 403.
     */
    public static SignedTransaction signType0(int chainTag, long blockRef, int expiration,
                                              List<Clause> clauses, int gasPriceCoef, long gas,
                                              long nonce, byte[] privateKey) {
        // VeChainThor signs the 9-field payload WITHOUT the signature field. Including an
        // empty signature (the Ethereum habit) yields a different digest, so thor recovers
        // a different sender whose VTHO balance is zero and rejects the tx with
        // "insufficient energy" — verified against the official SDK's golden vector.
        byte[] unsigned = encodeType0(chainTag, blockRef, expiration, clauses, gasPriceCoef, gas, nonce, null);
        byte[] txId = Hashes.blake2b256(unsigned);

        byte[] signature = signEcdsa(txId, privateKey);
        byte[] raw = encodeType0(chainTag, blockRef, expiration, clauses, gasPriceCoef, gas, nonce, signature);
        return new SignedTransaction(raw, txId);
    }

    /**
     * Builds the transaction {@code blockRef} from a block id: the first 8 bytes of the
     * block id, read as a big-endian unsigned 64-bit integer.
     *
     * <p>Verified against a real accepted testnet transaction whose RLP carried
     * {@code 018b58a7722a5ca4} while the containing block id started with
     * {@code 018b58a8…} — i.e. {@code blockId[0..8]}, not {@code blockNumber}.
     */
    public static long blockRefFromBlockId(byte[] blockId) {
        if (blockId == null || blockId.length < 8) {
            throw new IllegalArgumentException("A block id must be at least 8 bytes");
        }
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value = (value << 8) | (blockId[i] & 0xFFL);
        }
        return value;
    }

    /** Reinterprets a {@code long} as an unsigned 64-bit value for RLP integer encoding. */
    private static BigInteger asUnsigned(long value) {
        return value >= 0 ? BigInteger.valueOf(value) : BigInteger.valueOf(value).add(BigInteger.ONE.shiftLeft(64));
    }

    private static byte[] encodeType0(int chainTag, long blockRef, int expiration,
                                      List<Clause> clauses, int gasPriceCoef, long gas,
                                      long nonce, byte[] signature) {
        List<byte[]> encodedClauses = new ArrayList<>(clauses.size());
        for (Clause c : clauses) {
            encodedClauses.add(RlpEncoder.encodeList(List.of(
                    c.to() == null ? RlpEncoder.encodeNull() : RlpEncoder.encodeBytes(c.to()),
                    RlpEncoder.encodeBigInteger(c.value()),
                    RlpEncoder.encodeBytes(c.data() == null ? new byte[0] : c.data())
            )));
        }
        List<byte[]> fields = new ArrayList<>(10);
        fields.add(RlpEncoder.encodeBytes(new byte[]{(byte) chainTag}));
        // BlockRef is a uint64: minimal big-endian bytes of the 8-byte block id prefix,
        // encoded as an UNSIGNED integer (a signed long would overflow at 0x80…).
        fields.add(RlpEncoder.encodeBigInteger(asUnsigned(blockRef)));
        fields.add(RlpEncoder.encodeLong(expiration));
        fields.add(RlpEncoder.encodeList(encodedClauses));
        fields.add(RlpEncoder.encodeLong(gasPriceCoef));
        fields.add(RlpEncoder.encodeLong(gas));
        fields.add(RlpEncoder.encodeNull());                   // dependsOn
        fields.add(RlpEncoder.encodeLong(nonce));
        fields.add(RlpEncoder.encodeList(List.of()));          // reserved
        if (signature != null) {
            // Only the broadcast payload carries the signature; the signing hash does not.
            fields.add(RlpEncoder.encodeBytes(signature));
        }
        return RlpEncoder.encodeList(fields);
    }

    /** Deterministic (RFC 6979) ECDSA over secp256k1, canonical low-s, 65-byte r||s||v. */
    static byte[] signEcdsa(byte[] digest, byte[] privateKey) {
        ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        signer.init(true, new ECPrivateKeyParameters(new BigInteger(1, privateKey), CURVE));
        BigInteger e = new BigInteger(1, digest);
        BigInteger[] rs = signer.generateSignature(digest);
        BigInteger r = rs[0];
        BigInteger s = rs[1];
        if (s.compareTo(HALF_CURVE_ORDER) > 0) {
            s = CURVE.getN().subtract(s); // canonical low-s
        }
        int recId = recoverId(r, s, e, publicKeyPoint(privateKey));

        byte[] out = new byte[65];
        byte[] rBytes = toFixed32(r);
        byte[] sBytes = toFixed32(s);
        System.arraycopy(rBytes, 0, out, 0, 32);
        System.arraycopy(sBytes, 0, out, 32, 32);
        out[64] = (byte) recId;
        return out;
    }

    public static ECPoint publicKeyPoint(byte[] privateKey) {
        return CURVE.getG().multiply(new BigInteger(1, privateKey)).normalize();
    }

    /** Uncompressed public key without the 0x04 prefix (64 bytes). */
    public static byte[] publicKeyBytes(byte[] privateKey) {
        byte[] encoded = publicKeyPoint(privateKey).getEncoded(false);
        byte[] out = new byte[64];
        System.arraycopy(encoded, 1, out, 0, 64);
        return out;
    }

    private static int recoverId(BigInteger r, BigInteger s, BigInteger e, ECPoint expected) {
        for (int recId = 0; recId < 4; recId++) {
            ECPoint q = recoverPoint(r, s, e, recId);
            if (q != null && q.equals(expected)) {
                return recId;
            }
        }
        throw new IllegalStateException("Unable to compute ECDSA recovery id");
    }

    private static ECPoint recoverPoint(BigInteger r, BigInteger s, BigInteger e, int recId) {
        BigInteger n = CURVE.getN();
        BigInteger x = r.add(BigInteger.valueOf(recId / 2).multiply(n));
        if (x.compareTo(CURVE.getCurve().getField().getCharacteristic()) >= 0) {
            return null;
        }        ECPoint curvePoint = decompressPoint(x, (recId & 1) == 1);
        if (!curvePoint.multiply(n).isInfinity()) {
            return null;
        }
        BigInteger eInv = BigInteger.ZERO.subtract(e).mod(n);
        BigInteger rInv = r.modInverse(n);
        BigInteger srInv = rInv.multiply(s).mod(n);
        BigInteger eInvrInv = rInv.multiply(eInv).mod(n);
        return ECAlgorithms.sumOfTwoMultiplies(CURVE.getG(), eInvrInv, curvePoint, srInv).normalize();
    }

    /** Builds a SEC1 compressed point (0x02/0x03 prefix + 32-byte x) and decodes it. */
    private static ECPoint decompressPoint(BigInteger x, boolean yBit) {
        int byteLength = (CURVE.getCurve().getFieldSize() + 7) / 8;
        byte[] xBytes = toFixed(x, byteLength);
        byte[] encoded = new byte[1 + byteLength];
        encoded[0] = (byte) (yBit ? 0x03 : 0x02);
        System.arraycopy(xBytes, 0, encoded, 1, byteLength);
        return CURVE.getCurve().decodePoint(encoded);
    }

    private static byte[] toFixed(BigInteger value, int length) {
        byte[] bytes = value.toByteArray();
        byte[] out = new byte[length];
        if (bytes.length >= length) {
            System.arraycopy(bytes, bytes.length - length, out, 0, length);
        } else {
            System.arraycopy(bytes, 0, out, length - bytes.length, bytes.length);
        }
        return out;
    }

    private static byte[] toFixed32(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length == 32) {
            return bytes;
        }
        byte[] out = new byte[32];
        if (bytes.length > 32) {
            System.arraycopy(bytes, bytes.length - 32, out, 0, 32);
        } else {
            System.arraycopy(bytes, 0, out, 32 - bytes.length, bytes.length);
        }
        return out;
    }
}
