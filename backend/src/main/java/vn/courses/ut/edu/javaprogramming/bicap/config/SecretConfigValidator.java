package vn.courses.ut.edu.javaprogramming.bicap.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.util.Locale;

/**
 * Fail-fast validation of the deploy-time infrastructure configuration (BICAP-74/80/81).
 *
 * <p>Chạy <b>trước khi bean nào được tạo</b> (được gọi từ
 * {@link vn.courses.ut.edu.javaprogramming.bicap.common.security.SecureSecretInitializer},
 * một {@code ApplicationContextInitializer}) nên cấu hình sai sẽ dừng ứng dụng ngay với
 * thông báo rõ ràng, thay vì để Hibernate/Redis báo lỗi khó hiểu ở giai đoạn dựng bean.
 *
 * <p>Cấu hình production (mặc định, {@code ALLOW_SIMULATION=false}) yêu cầu:
 * <ul>
 *   <li>{@code SPRING_DATASOURCE_URL} trỏ tới MySQL remote (không H2, không placeholder);</li>
 *   <li>{@code BLOCKCHAIN_MODE=live} + {@code BLOCKCHAIN_PRIVATE_KEY} 32 byte hex +
 *       {@code BLOCKCHAIN_NODE_URL}; {@code BLOCKCHAIN_EXPORT_MODE=vechain}.</li>
 * </ul>
 * Test/CI đặt {@code ALLOW_SIMULATION=true} để bỏ qua hai nhóm kiểm tra này (H2 + mock chain).
 * Redis vẫn được kiểm tra thật khi khởi tạo cache (xem {@link RedisCacheConfig}).
 */
public final class SecretConfigValidator {

    private static final Logger log = LoggerFactory.getLogger(SecretConfigValidator.class);

    private SecretConfigValidator() {
    }

    public static void validateInfrastructure(Environment environment) {
        boolean allowSimulation = environment.getProperty("app.allow-simulation", Boolean.class, false);

        if (allowSimulation) {
            log.warn("ALLOW_SIMULATION=true — bỏ qua kiểm tra MySQL/blockchain (H2 + hash giả lập). "
                    + "Cấu hình này CHỈ dùng cho test/CI, không dùng cho production.");
            return;
        }

        validateDatasource(environment);
        validateBlockchain(environment);
    }

    /** Production dùng MySQL remote; H2 in-memory chỉ dành cho test/CI. */
    private static void validateDatasource(Environment environment) {
        String url = environment.getProperty("spring.datasource.url", "");
        String username = environment.getProperty("spring.datasource.username", "");
        String password = environment.getProperty("spring.datasource.password", "");

        if (isBlank(url)) {
            throw new IllegalStateException(
                    "SPRING_DATASOURCE_URL is not configured. Production yêu cầu MySQL remote, "
                            + "ví dụ jdbc:mysql://<host>:3306/bicap_db?useSSL=true&serverTimezone=UTC"
                            + "&allowPublicKeyRetrieval=true (xem .env.example).");
        }
        if (containsPlaceholder(url) || containsPlaceholder(username) || containsPlaceholder(password)) {
            throw new IllegalStateException(
                    "Cấu hình MySQL vẫn còn giá trị mẫu 'REPLACE_WITH_...'. Điền host/tài khoản/mật khẩu thật "
                            + "của server MySQL remote vào .env (SPRING_DATASOURCE_URL, "
                            + "SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD).");
        }
        if (url.trim().toLowerCase(Locale.ROOT).startsWith("jdbc:h2:")) {
            throw new IllegalStateException(
                    "SPRING_DATASOURCE_URL đang trỏ tới H2 in-memory — chỉ dùng cho test/CI. "
                            + "Production yêu cầu MySQL remote; nếu đây là test/CI hãy đặt ALLOW_SIMULATION=true.");
        }
    }

    private static void validateBlockchain(Environment environment) {
        String mode = environment.getProperty("blockchain.mode", "live");
        String exportMode = environment.getProperty("bicap.blockchain.export-mode", "vechain");
        String privateKey = environment.getProperty("blockchain.private-key", "");
        String nodeUrl = environment.getProperty("blockchain.node-url", "");

        if (!"live".equalsIgnoreCase(mode)) {
            throw new IllegalStateException(
                    "BLOCKCHAIN_MODE=" + mode + " là chế độ giả lập (hash sinh tại chỗ, không lên chain). "
                            + "Cấu hình production yêu cầu BLOCKCHAIN_MODE=live cùng BLOCKCHAIN_PRIVATE_KEY. "
                            + "Nếu đây là test/CI và bạn thật sự muốn giả lập, đặt ALLOW_SIMULATION=true.");
        }
        if ("local".equalsIgnoreCase(exportMode)) {
            throw new IllegalStateException(
                    "BLOCKCHAIN_EXPORT_MODE=local là stub SHA-256 (không neo lên VeChainThor). "
                            + "Đặt BLOCKCHAIN_EXPORT_MODE=vechain cho production, hoặc ALLOW_SIMULATION=true "
                            + "nếu chỉ chạy test/CI.");
        }
        if (isBlank(privateKey)) {
            throw new IllegalStateException(
                    "BLOCKCHAIN_PRIVATE_KEY is required when BLOCKCHAIN_MODE=live. "
                            + "Đặt private key hex của ví signer (sinh ví bằng dev/tools/WalletGen.java "
                            + "rồi nạp VTHO tại https://faucet.vecha.in).");
        }
        String key = privateKey.trim();
        if (key.startsWith("0x") || key.startsWith("0X")) {
            key = key.substring(2);
        }
        if (!key.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalStateException(
                    "BLOCKCHAIN_PRIVATE_KEY không hợp lệ: phải là 32 byte hex (64 ký tự, có thể kèm tiền tố 0x).");
        }
        if (isBlank(nodeUrl)) {
            throw new IllegalStateException(
                    "BLOCKCHAIN_NODE_URL is required when BLOCKCHAIN_MODE=live (ví dụ https://testnet.vechain.org).");
        }

        log.info("Deploy-time configuration validated (MySQL remote, blockchain=live node={})", nodeUrl);
    }

    private static boolean containsPlaceholder(String value) {
        return value != null && value.toUpperCase(Locale.ROOT).contains("REPLACE_WITH");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
