package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * Request payload for adding a certification/business-license document to a farm (BICAP-9 / SRS-FM-003).
 * Dùng cho endpoint POST /api/farms/{id}/documents.
 *
 * type    — loại giấy tờ, ví dụ: "BUSINESS_LICENSE", "VietGAP", "GlobalGAP", "Organic"
 * fileUrl — URL trỏ đến file giấy phép/chứng nhận (PDF hoặc ảnh đã được upload lên CDN/storage)
 * expiryDate — ngày hết hạn của giấy tờ
 */
public class AddCertificationRequest {

    @NotBlank(message = "Loại chứng nhận không được để trống")
    @Size(max = 100, message = "Loại chứng nhận không vượt quá 100 ký tự")
    private String type;

    @NotBlank(message = "URL file không được để trống")
    @Size(max = 500, message = "URL file không vượt quá 500 ký tự")
    private String fileUrl;

    @NotNull(message = "Ngày hết hạn không được để trống")
    @FutureOrPresent(message = "Ngày hết hạn không được là ngày trong quá khứ")
    private LocalDate expiryDate;

    public AddCertificationRequest() {}

    public AddCertificationRequest(String type, String fileUrl, LocalDate expiryDate) {
        this.type = type;
        this.fileUrl = fileUrl;
        this.expiryDate = expiryDate;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
}
