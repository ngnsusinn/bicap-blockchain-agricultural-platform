package vn.courses.ut.edu.javaprogramming.bicap;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;
import vn.courses.ut.edu.javaprogramming.bicap.service.QrCodeService;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrCodeServiceTest {
    @Test
    void generatedQrDecodesToTraceUrlUsingTransactionHash() throws Exception {
        String transactionHash = "0x" + "a1".repeat(32);
        QrCodeService service = new QrCodeService("https://bicap.vn/");

        String dataUri = service.pngDataUri(transactionHash);
        assertTrue(dataUri.startsWith("data:image/png;base64,"));

        byte[] png = Base64.getDecoder().decode(dataUri.substring(dataUri.indexOf(',') + 1));
        var image = ImageIO.read(new ByteArrayInputStream(png));
        var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));

        assertEquals("https://bicap.vn/trace/" + transactionHash,
                new MultiFormatReader().decode(bitmap).getText());
    }
}
