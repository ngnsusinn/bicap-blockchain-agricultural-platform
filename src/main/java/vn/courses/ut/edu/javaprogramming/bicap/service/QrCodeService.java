package vn.courses.ut.edu.javaprogramming.bicap.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class QrCodeService {

    /**
     * Tạo mã QR Code từ chuỗi nội dung (text)
     * @param text Nội dung cần mã hóa thành QR Code (VD: link truy xuất)
     * @param width Chiều rộng ảnh
     * @param height Chiều cao ảnh
     * @return Mảng byte chứa dữ liệu ảnh định dạng PNG
     */
    public byte[] generateQrCodeImage(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            
            // Cấu hình hint để hỗ trợ tiếng Việt (UTF-8) và bỏ viền trắng thừa
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            
            // Tạo ma trận điểm ảnh (BitMatrix) từ chuỗi text
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);
            
            // Ghi ma trận ra stream dưới dạng ảnh PNG
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            
            return pngOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo QR Code: " + e.getMessage(), e);
        }
    }
}
