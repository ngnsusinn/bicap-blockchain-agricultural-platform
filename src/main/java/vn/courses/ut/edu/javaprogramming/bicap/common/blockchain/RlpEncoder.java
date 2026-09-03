package vn.courses.ut.edu.javaprogramming.bicap.common.blockchain;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.List;

/**
 * Minimal Recursive Length Prefix (RLP) encoder — the canonical serialization used by
 * VeChainThor (and Ethereum) for transaction payloads. Supports byte strings, nested
 * lists, and non-negative integers (minimal big-endian form).
 */
public final class RlpEncoder {

    private RlpEncoder() {}

    public static byte[] encodeBytes(byte[] value) {
        if (value.length == 1 && (value[0] & 0xFF) < 0x80) {
            return value.clone();
        }
        return concat(header(value.length, 0x80), value);
    }

    /** Encodes a non-negative integer in minimal big-endian form (0 → empty string). */
    public static byte[] encodeLong(long value) {
        return encodeBigInteger(BigInteger.valueOf(value));
    }

    public static byte[] encodeBigInteger(BigInteger value) {
        if (value.signum() < 0) {
            throw new IllegalArgumentException("RLP integers must be non-negative");
        }
        if (value.signum() == 0) {
            return encodeBytes(new byte[0]);
        }
        byte[] bytes = value.toByteArray();
        // BigInteger prepends a 0x00 sign byte when the high bit is set — strip it.
        if (bytes[0] == 0) {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            bytes = trimmed;
        }
        return encodeBytes(bytes);
    }

    /** Encodes a list from already-encoded items. */
    public static byte[] encodeList(List<byte[]> items) {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        for (byte[] item : items) {
            payload.writeBytes(item);
        }
        byte[] body = payload.toByteArray();
        return concat(header(body.length, 0xC0), body);
    }

    /** Empty payload (RLP null / dependsOn absent). */
    public static byte[] encodeNull() {
        return encodeBytes(new byte[0]);
    }

    private static byte[] header(int length, int offset) {
        if (length <= 55) {
            return new byte[]{(byte) (offset + length)};
        }
        byte[] lenBytes = bigEndian(length);
        return concat(new byte[]{(byte) (offset + 55 + lenBytes.length)}, lenBytes);
    }

    private static byte[] bigEndian(int value) {
        byte[] out = new byte[4];
        int i = 4;
        int v = value;
        while (v > 0) {
            out[--i] = (byte) (v & 0xFF);
            v >>>= 8;
        }
        byte[] trimmed = new byte[4 - i];
        System.arraycopy(out, i, trimmed, 0, trimmed.length);
        return trimmed;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
