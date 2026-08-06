package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class VerificationEmailService {
    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String frontendUrl;

    public VerificationEmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String fromAddress,
            @Value("${app.frontend.url:http://localhost:5174}") String frontendUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.frontendUrl = frontendUrl;
    }

    public void sendRetailerVerification(String recipient, String token) {
        String separator = frontendUrl.contains("?") ? "&" : "?";
        String verificationUrl = frontendUrl + separator + "verifyToken=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject("Xác nhận tài khoản Nhà bán lẻ BICAP");
        message.setText("""
                Chào mừng bạn đến với BICAP.

                Vui lòng xác nhận tài khoản Nhà bán lẻ bằng liên kết sau:
                %s

                Liên kết có hiệu lực trong 24 giờ. Nếu bạn không thực hiện đăng ký này,
                hãy bỏ qua email.
                """.formatted(verificationUrl));
        mailSender.send(message);
    }
}
