package vn.courses.ut.edu.javaprogramming.bicap.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.courses.ut.edu.javaprogramming.bicap.service.QrCodeService;

@RestController
@RequestMapping("/api/qrcode")
public class QrCodeController {

    @Autowired
    private QrCodeService qrCodeService;

    /**
     * API tạo QR Code
     * GET /api/qrcode/generate?productId=123&txId=0xabc...
     */
    @GetMapping(value = "/generate")
    public ResponseEntity<byte[]> generateQrCode(
            @RequestParam("productId") Long productId,
            @RequestParam("txId") String txId) {
        
        // Link dùng để consumer (khách hàng) quét và xem thông tin sản phẩm
        // Domain này sẽ là domain thực tế khi deploy (VD: https://bicap.vn/trace/...)
        String trackingUrl = "https://bicap.vn/trace?product=" + productId + "&tx=" + txId;
        
        // Kích thước chuẩn cho QR Code
        int width = 300;
        int height = 300;
        
        byte[] imageBytes = qrCodeService.generateQrCodeImage(trackingUrl, width, height);
        
        // Đặt header để browser hiểu đây là ảnh PNG
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        
        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }
}
