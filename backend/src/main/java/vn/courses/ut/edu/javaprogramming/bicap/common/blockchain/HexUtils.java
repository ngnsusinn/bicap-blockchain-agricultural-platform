package vn.courses.ut.edu.javaprogramming.bicap.common.blockchain;

/** Hex helpers for VeChainThor payloads (0x-prefixed lowercase). */
public final class HexUtils {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private HexUtils() {}

    public static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2 + 2];
        out[0] = '0';
        out[1] = 'x';
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[2 + i * 2] = HEX[v >>> 4];
            out[3 + i * 2] = HEX[v & 0x0F];
        }
        return new String(out);
    }

    public static byte[] fromHex(String hex) {
        String s = hex.startsWith("0x") || hex.startsWith("0X") ? hex.substring(2) : hex;
        if (s.length() % 2 != 0) s = "0" + s;
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}
