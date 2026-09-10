package vn.courses.ut.edu.javaprogramming.bicap.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Service
public class QrCodeService {
    private final String frontendUrl;
    public QrCodeService(@Value("${app.frontend.url:http://localhost:5174}") String frontendUrl) {
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }
    public String traceUrl(String traceHash) { return frontendUrl + "/trace/" + traceHash; }
    public String pngDataUri(String traceHash) {
        try {
            var matrix = new QRCodeWriter().encode(traceUrl(traceHash), BarcodeFormat.QR_CODE, 320, 320);
            var output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception ex) { throw new IllegalStateException("Cannot generate QR code", ex); }
    }
}
