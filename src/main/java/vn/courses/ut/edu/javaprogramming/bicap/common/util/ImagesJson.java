package vn.courses.ut.edu.javaprogramming.bicap.common.util;

import java.util.List;

/**
 * Đóng/mở JSON array các đường dẫn ảnh sản phẩm lưu ở cột {@code products.images}
 * (BICAP-18 / SRS-FM-012). Không phụ thuộc thư viện JSON bên ngoài — format giữ
 * ở dạng mảng đơn giản {@code ["/uploads/...", ...]}.
 */
public final class ImagesJson {

    private ImagesJson() {}

    /** Mã hoá danh sách URL thành chuỗi JSON array. Null/empty → {@code null}. */
    public static String toJson(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < urls.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(urls.get(i).replace("\"", "\\\"")).append('"');
        }
        return sb.append(']').toString();
    }

    /** Giải mã chuỗi JSON array về danh sách URL. Null/empty → danh sách rỗng. */
    public static List<String> parse(String json) {
        if (json == null || json.isBlank() || !json.trim().startsWith("[")) {
            return List.of();
        }
        String body = json.trim().substring(1, json.trim().lastIndexOf(']'));
        if (body.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(body.split(","))
                .map(String::trim)
                .map(s -> s.replaceAll("^\"|\"$", "").replace("\\\"", "\""))
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
