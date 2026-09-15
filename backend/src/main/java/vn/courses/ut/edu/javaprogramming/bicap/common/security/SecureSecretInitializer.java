package vn.courses.ut.edu.javaprogramming.bicap.common.security;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import vn.courses.ut.edu.javaprogramming.bicap.config.SecretConfigValidator;

import java.util.Locale;
import java.util.Set;

/**
 * C-1 / C-3: enforces that every deploy-time secret is a real, explicitly configured value.
 *
 * <p>The repository used to ship {@code app.jwt.secret} and {@code sepay.api-key} with
 * hard-coded fallback values in {@code application.properties}. Because those values live
 * in a public Git repository, anybody could mint a valid {@code SUPER_ADMIN} JWT (or forge
 * a Sepay payment webhook) against any deployment that did not override them.
 *
 * <p>This initializer runs before any bean is created and applies the production rule to
 * <b>every</b> profile:
 * <ol>
 *   <li>a <b>missing</b> secret is a fatal configuration error — there is no ephemeral /
 *       generated dev secret anymore, so an unconfigured deployment stops instead of
 *       quietly running with throwaway key material;</li>
 *   <li>a <b>publicly known</b> secret (the previously shipped defaults and the documented
 *       placeholders) is rejected outright.</li>
 * </ol>
 *
 * <p>Registered through {@code META-INF/spring.factories} so it applies to
 * {@code SpringApplication.run} and to {@code @SpringBootTest} alike (tests provide their
 * own throwaway key material in {@code src/test/resources/application.properties}).
 */
public class SecureSecretInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    public static final String JWT_SECRET_PROPERTY = "app.jwt.secret";
    public static final String SEPAY_KEY_PROPERTY = "sepay.api-key";

    /** Secrets that are published in this repository / docs and must never be accepted. */
    public static final Set<String> INSECURE_JWT_SECRETS = Set.of(
            // previous application.properties default ("test-jwt-secret-key-at-least-32-bytes")
            "dGVzdC1qd3Qtc2VjcmV0LWtleS1hdC1sZWFzdC0zMi1ieXRlcw==",
            // previous SecretConfigValidator block-list entries
            "dGVzdF9zdXBlcl9zZWNyZXRfa2V5X3doaWNoX2lzX2F0X2xlYXN0XzMyX2J5dGVzX2xvbmc=",
            "defaultSecretKeyWhichShouldBeAtLeast32BytesLongForHS256Algorithm",
            // .env.example template value
            "replace-with-a-random-base64-value-of-at-least-32-bytes");

    /** Sepay webhook keys that are published in this repository / docs. */
    public static final Set<String> INSECURE_SEPAY_KEYS = Set.of(
            "test-sepay-api-key",
            "replace-with-your-real-sepay-api-key",
            "YOUR_SEPAY_API_KEY");

    /** Substrings that mark a value as an unreplaced template/placeholder. */
    private static final String[] PLACEHOLDER_MARKERS = {
            "replace-with", "replace_with", "your-", "your_", "changeme", "change-me",
            "placeholder", "example.com", "todo"};

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        ConfigurableEnvironment environment = context.getEnvironment();

        requireSecret(environment, JWT_SECRET_PROPERTY, "JWT_SECRET", "openssl rand -base64 48");
        requireSecret(environment, SEPAY_KEY_PROPERTY, "SEPAY_API_KEY", "openssl rand -hex 32");

        // Cấu hình hạ tầng (MySQL remote / blockchain live) được kiểm tra ở đây — TRƯỚC khi
        // bean nào được tạo — để lỗi cấu hình hiện ra rõ ràng thay vì để Hibernate báo
        // "Unable to determine Dialect without JDBC metadata" khó hiểu.
        SecretConfigValidator.validateInfrastructure(environment);
    }

    private void requireSecret(ConfigurableEnvironment environment,
                               String property,
                               String envVarName,
                               String generationHint) {
        String value = environment.getProperty(property);
        String normalized = value == null ? "" : value.trim();

        if (normalized.isEmpty()) {
            throw new IllegalStateException(envVarName + " is not configured. This application no longer "
                    + "generates a development secret: set the " + envVarName + " environment variable "
                    + "(" + generationHint + ") before starting. See .env.example.");
        }

        if (isInsecureSecret(property, normalized)) {
            throw new IllegalStateException(envVarName + " is set to a publicly known default/placeholder value. "
                    + "Generate a fresh secret (" + generationHint + ") and set the " + envVarName
                    + " environment variable before starting the application.");
        }
    }

    /** Exposed for tests: checks whether a value is a publicly known insecure secret. */
    public static boolean isInsecureSecret(String property, String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim();
        if (JWT_SECRET_PROPERTY.equals(property)) {
            return INSECURE_JWT_SECRETS.contains(normalized) || looksLikePlaceholder(normalized);
        }
        if (SEPAY_KEY_PROPERTY.equals(property)) {
            return INSECURE_SEPAY_KEYS.contains(normalized) || looksLikePlaceholder(normalized);
        }
        return false;
    }

    /** A template value such as {@code replace-with-...} or {@code your_api_key}. */
    private static boolean looksLikePlaceholder(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        for (String marker : PLACEHOLDER_MARKERS) {
            if (lower.contains(marker)) {
                return true;
            }
        }
        return false;
    }
}
