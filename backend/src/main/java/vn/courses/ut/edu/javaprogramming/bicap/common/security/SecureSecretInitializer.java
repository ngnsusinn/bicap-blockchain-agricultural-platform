package vn.courses.ut.edu.javaprogramming.bicap.common.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.Profiles;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * C-1 / C-3 (fixed): removes the "publicly known default secret" backdoor.
 *
 * <p>The repository used to ship {@code app.jwt.secret} and {@code sepay.api-key} with
 * hard-coded fallback values in {@code application.properties}. Because those values live
 * in a public Git repository, anybody could mint a valid {@code SUPER_ADMIN} JWT (or forge
 * a Sepay payment webhook) against any deployment that did not override them.
 *
 * <p>This initializer runs before any bean is created and enforces three rules:
 * <ol>
 *   <li>a <b>publicly known</b> secret (the previously shipped defaults and the documented
 *       placeholders) is rejected outright — the application refuses to boot;</li>
 *   <li>in a <b>production</b> profile ({@code prod}/{@code production}) a missing secret is a
 *       fatal configuration error;</li>
 *   <li>in development/test a missing secret is replaced with a fresh, per-boot
 *       cryptographically random value so local runs still work without env vars. Tokens
 *       generated with an ephemeral secret die with the process — set the env var for a
 *       stable secret.</li>
 * </ol>
 *
 * <p>Registered through {@code META-INF/spring.factories} so it applies to
 * {@code SpringApplication.run} and to {@code @SpringBootTest} alike.
 */
public class SecureSecretInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger log = LoggerFactory.getLogger(SecureSecretInitializer.class);
    private static final SecureRandom RANDOM = new SecureRandom();

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
        boolean production = environment.acceptsProfiles(Profiles.of("prod", "production"));
        Map<String, Object> generated = new HashMap<>();

        resolveSecret(environment, JWT_SECRET_PROPERTY, production, generated,
                "JWT_SECRET", 48,
                "openssl rand -base64 48");

        resolveSecret(environment, SEPAY_KEY_PROPERTY, production, generated,
                "SEPAY_API_KEY", 32,
                "openssl rand -hex 32");

        if (!generated.isEmpty()) {
            MutablePropertySources sources = environment.getPropertySources();
            sources.addFirst(new MapPropertySource("bicapEphemeralDevSecrets", generated));
        }
    }

    private void resolveSecret(ConfigurableEnvironment environment,
                               String property,
                               boolean production,
                               Map<String, Object> generated,
                               String envVarName,
                               int randomBytes,
                               String generationHint) {
        String value = environment.getProperty(property);
        String normalized = value == null ? "" : value.trim();

        if (normalized.isEmpty()) {
            if (production) {
                throw new IllegalStateException(envVarName + " is not configured. Set the " + envVarName
                        + " environment variable (" + generationHint + ") before starting in production.");
            }
            generated.put(property, randomBase64(randomBytes));
            log.warn("{} is not configured — generated an ephemeral development secret for this process only. "
                    + "Tokens/secrets are invalidated on restart; set {} for a stable value.",
                    envVarName, envVarName);
            return;
        }

        if (isInsecureSecret(property, normalized)) {
            throw new IllegalStateException(envVarName + " is set to a publicly known default/placeholder value. "
                    + "Generate a fresh secret (" + generationHint + ") and set the " + envVarName
                    + " environment variable before starting the application.");
        }
    }

    private static String randomBase64(int bytes) {
        byte[] buffer = new byte[bytes];
        RANDOM.nextBytes(buffer);
        return Base64.getEncoder().encodeToString(buffer);
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

    /** A template value such as {@code replace-with-...} or {@code your_smtp_username}. */
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
