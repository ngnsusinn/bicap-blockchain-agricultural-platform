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
 * <p>Payload layout (RLP):
 * {@code [ chainTag, blockRef, expiration, [[to, value, data]...], gasPrice, gas,
 * dependsOn, nonce, signature ]}. The transaction ID is the Blake2b-256 hash of the
 * payload with an empty signature — which is also the digest that gets signed, so
 * {@code id == signingHash}.
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
     */
    public static SignedTransaction signType0(int chainTag, long blockNumber, int expiration,
                                              List<Clause> clauses, int gasPriceCoef, long gas,
                                              long nonce, byte[] privateKey) {
        byte[] unsigned = encodeType0(chainTag, blockNumber, expiration, clauses, gasPriceCoef, gas, nonce, new byte[0]);
        byte[] txId = Hashes.blake2b256(unsigned);

        byte[] signature = signEcdsa(txId, privateKey);
        byte[] raw = encodeType0(chainTag, blockNumber, expiration, clauses, gasPriceCoef, gas, nonce, signature);
        return new SignedTransaction(raw, txId);
    }

    private static byte[] encodeType0(int chainTag, long blockNumber, int expiration,
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
        return RlpEncoder.encodeList(List.of(
                RlpEncoder.encodeBytes(new byte[]{(byte) chainTag}),
                // BlockRef is a uint64 (canonical RLP integer) = the reference block number.
                RlpEncoder.encodeLong(blockNumber),
                RlpEncoder.encodeLong(expiration),
                RlpEncoder.encodeList(encodedClauses),
                RlpEncoder.encodeLong(gasPriceCoef),
                RlpEncoder.encodeLong(gas),
                RlpEncoder.encodeNull(),                       // dependsOn
                RlpEncoder.encodeLong(nonce),
                RlpEncoder.encodeList(List.of()),              // Reserved — empty list
                RlpEncoder.encodeBytes(signature)
        ));
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
